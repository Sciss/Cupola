/*
 *  Cupola.scala
 *  (Cupola)
 *
 *  Copyright (c) 2010 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either
 *  version 2, june 1991 of the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License (gpl.txt) along with this software; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 *
 *
 *  Changelog:
 */

package de.sciss.cupola

import de.sciss.synth.swing.{ NodeTreePanel, ServerStatusPanel }
import actors.{ Actor, DaemonActor, OutputChannel }
import collection.mutable.{ HashSet => MHashSet }
import java.awt.{GraphicsEnvironment, EventQueue}
import de.sciss.synth._
import de.sciss.nuages.{NuagesFrame, NuagesConfig}
import proc.{ DSL, ProcDemiurg, ProcTxn, Ref, TxnModel }
import DSL._
import de.sciss.osc._
import java.net.{InetSocketAddress, SocketAddress}
import java.util.Properties
import java.io.{FileOutputStream, FileInputStream, File, RandomAccessFile}

//case class CupolaUpdate( stage: Option[ (Level, Section) ])
//case class CupolaUpdate( stage: Option[ Double ])
case class CupolaUpdate( stage: Option[ Stage ], dist: Option[ Double ])

/**
 *    @version 0.13, 10-Oct-10
 */
object Cupola extends TxnModel[ CupolaUpdate ] {

   type Update    = CupolaUpdate
   type Listener  = L

   private val PROP_BASEPATH  = "basepath"
   private val PROP_SCPATH    = "supercollider"

   val properties          = {
      val file = new File( "cupola-settings.xml" )
      val prop = new Properties()
      if( file.isFile ) {
         val is = new FileInputStream( file )
         prop.loadFromXML( is )
         is.close
      } else {
         prop.setProperty( PROP_BASEPATH,
            new File( new File( System.getProperty( "user.home" ), "Desktop" ), "Cupola" ).getAbsolutePath )
         prop.setProperty( PROP_SCPATH,
            new File( new File( new File( new File( new File( System.getProperty( "user.home" ), "Documents" ),
               "devel" ), "SuperCollider3" ), "common" ), "build" ).getAbsolutePath )
         val os = new FileOutputStream( file )
         prop.storeToXML( os, "Cupola Settings" )
         os.close
      }
      prop
   }

   val fs                  = File.separator
   val BASE_PATH           = properties.getProperty( PROP_BASEPATH )
   val AUDIO_PATH          = BASE_PATH + fs + "audio_work" + fs + "material"
   val OSC_PATH            = BASE_PATH + fs + "osc"
   val REC_PATH            = BASE_PATH + fs + "rec"
   val DUMP_OSC            = false
   val NUAGES_ANTIALIAS    = false
   val INTERNAL_AUDIO      = false
   val NODE_TREE_PANEL     = false
   val MASTER_NUMCHANNELS  = 2 // 4
   val MASTER_OFFSET       = 0
   val MIC_OFFSET          = 0
   val TRACKING_PORT       = 1201
   val TRACKING_PROTO      = TCP
   val TRACKING_LOOP       = true
   val TRACKING_CONNECT    = false
   val SHOW_VIS            = false
   var masterBus : AudioBus = null

   lazy val SCREEN_BOUNDS =
         GraphicsEnvironment.getLocalGraphicsEnvironment.getDefaultScreenDevice.getDefaultConfiguration.getBounds

   private var vis: TrackingVis = null
   @volatile var trackingDefeated = false

   private var stageRef: Ref[ Stage ] = Ref( IdleStage ) // Ref[ (Level, Section) ]( UnknownLevel -> Section1 )
   private var distRef = Ref( 0.0 )

   private val trackingAddr      = new InetSocketAddress( "127.0.0.1", TRACKING_PORT )
   private val tracking          = {
      val res = OSCClient( TRACKING_PROTO, 0, TRACKING_LOOP, OSCTrackingCodec )
      res.action = messageReceived
      res.target = trackingAddr
      if( TRACKING_CONNECT ) res.start
      res
//      val res = JOSCServer.newUsing( TRACKING_PROTO, TRACKING_PORT, TRACKING_LOOP )
//      res.addOSCListener( new JOSCListener {
//         def messageReceived( jmsg: JOSCMessage, addr: SocketAddress, time: Long ) {
//            val args = (0 until jmsg.getArgCount()) map { i => jmsg.getArg( i ) /* match {
//               case f: java.lang.Float    => f.floatValue
//               case i: java.lang.Integer  => i.intValue
//               case x => x
//            } */
//            }
//            val msg  = OSCMessage( jmsg.getName(), args: _* )
//            Cupola.messageReceived( msg, addr, time )
//         }
//      })
//      res.dumpOSC( 1, System.out )
//      res.start()
//      res
   }
   private val trackingLocalAddr = tracking.localAddress

//   private val simulator         = {
//      val trns = OSCTransmitter( TRACKING_PROTO )
//      trns.target = tracking.getLocalAddress() // tracking.localAddress
////      trns.dumpOSC( 1, System.out )
//      trns.connect
//      trns
//   }
//   private val listeners         = new MHashSet[ OutputChannel[ Any ]]

   val options          = {
      val o = new ServerOptionsBuilder()
      if( INTERNAL_AUDIO ) {
         o.deviceNames        = Some( "Built-in Microphone" -> "Built-in Output" )
      } else {
         o.deviceName         = Some( "MOTU 828mk2" )
      }
      o.inputBusChannels   = 10
      o.outputBusChannels  = 10
      o.audioBusChannels   = 512
      o.loadSynthDefs      = false
      o.memorySize         = 65536
      o.zeroConf           = false
      o.programPath        = properties.getProperty( PROP_SCPATH ) + File.separator + "scsynth"
      o.build
   }

   val support = new REPLSupport
   val pm      = new ProcessManager2

   @volatile var s: Server       = _
   @volatile var booting: ServerConnection = _
   @volatile var config: NuagesConfig = _

   def main( args: Array[ String ]) {
//      s.options.programPath.value = "/Users/rutz/Documents/devel/fromSVN/SuperCollider3/common/build/scsynth"
//      s.addDoWhenBooted( this ! Run ) // important: PlainServer executes this in the OSC receiver thread, so fork!
//      start
      guiRun { init }
   }

//   private def initGUI {
//      val sspw = new ServerStatusPanel( s ).makeWindow
//      val ntp  = new NodeTreePanel( s )
//      val ntpw = ntp.makeWindow
//      ntpw.setLocation( sspw.getX, sspw.getY + sspw.getHeight + 32 )
//      val sif  = new ScalaInterpreterFrame( s, ntp )
//      sif.setLocation( sspw.getX + sspw.getWidth + 32, sif.getY )
//
//      sspw.setVisible( true )
//      ntpw.setVisible( true )
//      sif.setVisible( true )
//   }

   protected def emptyUpdate = CupolaUpdate( None, None )
   protected def fullUpdate( implicit tx: ProcTxn ) = CupolaUpdate( Some( stageRef() ), Some( distRef() ))

   def guiRun( code: => Unit ) {
      EventQueue.invokeLater( new Runnable { def run = code })
   }

//   def simulate( msg: OSCMessage ) { simulator ! msg }
   def simulateRemote( p: OSCPacket ) { try { tracking ! p } catch { case _ => }}
   def simulateLocal( p: OSCPacket ) {
      simulateLocal( p, trackingLocalAddr, System.currentTimeMillis )
   }
   private def simulateLocal( p: OSCPacket, addr: SocketAddress, time: Long ) {
      p match {
         case m: OSCMessage => messageReceived( m, addr, time )
         case b: OSCBundle => b.packets.foreach( simulateLocal( _, addr, b.timetag ))
      }
   }

   def simulateBoth( p: OSCPacket ) {
      if( tracking.isActive ) simulateRemote( p ) else simulateLocal( p )
   }

//   def defeatTracking( defeat: Boolean ) { trackingDefeatedVar = defeat }
   def trackingConnected = tracking.isActive
   def trackingConnected_=( connect: Boolean ) {
      if( connect ) {
         if( !tracking.isActive ) {
            tracking.start
            tracking ! OSCMessage( "/notify", 1 )
         }
      } else {
         if( tracking.isActive ) {
            tracking ! OSCMessage( "/notify", 0 )
            tracking.stop
         }
      }
   }

   def dumpOSC( mode: Int ) { tracking.dumpIncomingOSC( mode )}

   private def messageReceived( msg: OSCMessage, addr: SocketAddress, time: Long ) {
      if( trackingDefeated && (addr != trackingLocalAddr) ) {
         println( "defeated : " + addr + " != " + trackingLocalAddr )
         return
      }
      msg match {
//         case t: OSCTrackingMessage => {
//            stageChange( Some( t.state / 8.0 ))
//            if( vis != null ) vis.update( t )
//         }
//         case OSCMessage( "/cupola", "state", scale: Float ) => stageChange( Some( scale.toDouble ))
         case OSCDistMessage( dist ) => ProcTxn.spawnAtomic { implicit tx => distChange( dist )}
         case OSCStageMessage( stageID ) =>
            Stage.all.find( _.id == stageID ) match {
               case Some( stage ) => ProcTxn.spawnAtomic { implicit tx => stageChange( stage )}
               case None => println( "!! Cupola: Ignoring illegal stage ID " + stageID ) 
            }
         case x => println( "!! Cupola: Ignoring OSC message '" + x + "'" )
      }
   }

   private def stageChange( newStage: Stage )( implicit tx: ProcTxn ) {
      val oldStage = stageRef.swap( newStage )
      if( oldStage != newStage ) {
         touch
         val u = updateRef()
         updateRef.set( u.copy( stage = Some( newStage )))
      }
      pm.stageChange( oldStage, newStage ) // even if equal, to allow proper init
   }

   private def distChange( newDist: Double )( implicit tx: ProcTxn ) {
//println( "DISTCHANGE" + newDist )
      distRef.set( newDist )
      touch
      val u = updateRef()
      updateRef.set( u.copy( dist = Some( newDist )))
      pm.distChange( newDist )
   }

   def init {
      // prevent actor starvation!!!
      // --> http://scala-programming-language.1934581.n4.nabble.com/Scala-Actors-Starvation-td2281657.html
      System.setProperty( "actors.enableForkJoin", "false" )

      val sif  = new ScalaInterpreterFrame( support /* ntp */ )
      val ssp  = new ServerStatusPanel()
      val sspw = ssp.makeWindow
      val ntpo: Option[ NodeTreePanel ] = if( NODE_TREE_PANEL ) {
         val ntp = new NodeTreePanel()
         val ntpw = ntp.makeWindow
         ntpw.setLocation( sspw.getX, sspw.getY + sspw.getHeight + 32 )
         ntpw.setVisible( true )
         Some( ntp )
      } else None
      sspw.setVisible( true )
      sif.setLocation( sspw.getX + sspw.getWidth + 32, sif.getY )
      sif.setVisible( true )
      booting = Server.boot( options = options ) {
         case ServerConnection.Preparing( srv ) => {
            ssp.server = Some( srv )
            ntpo.foreach( _.server = Some( srv ))
         }
         case ServerConnection.Running( srv ) => {
            ProcDemiurg.addServer( srv )
            s = srv
            support.s = srv

            if( DUMP_OSC ) s.dumpOSC(1)

            // nuages
            initNuages
            new GUI
            if( SHOW_VIS ) vis = new TrackingVis
         }
      }
      Runtime.getRuntime().addShutdownHook( new Thread { override def run = shutDown })
   }

   private def initNuages {
      masterBus  = if( INTERNAL_AUDIO ) {
         new AudioBus( s, 0, 2 )
      } else {
         new AudioBus( s, MASTER_OFFSET, MASTER_NUMCHANNELS )
      }
      val soloBus    = Bus.audio( s, 2 )
      config         = NuagesConfig( s, Some( masterBus ), Some( soloBus ), Some( REC_PATH ))
      val f          = new NuagesFrame( config )
      f.panel.display.setHighQuality( NUAGES_ANTIALIAS )
      f.setSize( 640, 480 )
      f.setLocation( ((SCREEN_BOUNDS.width - f.getWidth()) >> 1) + 100, 10 )
      f.setVisible( true )
      support.nuages = f
      ProcTxn.atomic { implicit tx =>
         CupolaNuages.init( s )
//         pm.init
         stageChange( IdleStage )
      }
   }

   def quit { System.exit( 0 )}

   private def shutDown { // sync.synchronized { }
      if( (s != null) && (s.condition != Server.Offline) ) {
         s.quit
         s = null
      }
      if( booting != null ) {
         booting.abort
         booting = null
      }
//      simulator.dispose
      if( tracking.isActive ) {
         try {
            tracking ! OSCMessage( "/notify", 0 )
            tracking ! OSCMessage( "/dumpOSC", 0 )
         } catch { case _ => }
      }
      tracking.dispose
    }
}