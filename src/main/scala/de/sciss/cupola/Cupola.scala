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

import java.net.SocketAddress
import java.awt.EventQueue
import de.sciss.synth.swing.{ NodeTreePanel, ServerStatusPanel }
import actors.{ Actor, DaemonActor, OutputChannel }
import de.sciss.scalaosc.{ OSCMessage, OSCReceiver, OSCTransmitter, UDP }
import de.sciss.synth.{ BootingServer, Server }
import collection.mutable.{ HashSet => MHashSet }
import de.sciss.synth.proc.ProcDemiurg
import de.sciss.smc.{SMCNuages, SMC}
import com.jhlabs.jnitablet.TabletWrapper

/**
 *    @version 0.11, 21-Jun-10
 */
object Cupola extends Actor {
   import Actor._

   // messages received by this object
   case object Run
   case object Quit
   case object AddListener
   case object RemoveListener
   case object QueryLevel

   // messages sent out by this object to listeners
   case class LevelChanged( newLevel: Level, newSection: Section )

   val trackingPort              = 0x6375
   
   private var level: Level      = UnknownLevel
   private var section: Section  = Section1
//   private val tracking          = {
//      val rcv = OSCReceiver( UDP, trackingPort )
//      rcv.action = messageReceived
//      rcv.start
//      rcv
//   }
//   private val simulator         = {
//      val trns = OSCTransmitter( UDP )
//      trns.target = tracking.localAddress
//      trns.connect
//      trns
//   }
   private val listeners         = new MHashSet[ OutputChannel[ Any ]]

   def main( args: Array[ String ]) {
//      s.options.programPath.value = "/Users/rutz/Documents/devel/fromSVN/SuperCollider3/common/build/scsynth"
//      s.addDoWhenBooted( this ! Run ) // important: PlainServer executes this in the OSC receiver thread, so fork!
//      start
//      guiRun { init }

      // there is something tricky about the point of time
      // at which the JNI lib gets loaded... seems the safest
      // is to do that before AWT initializes...
      if( SMCNuages.USE_TABLET ) TabletWrapper.getInstance()
      guiRun { SMC.run }
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

   def guiRun( code: => Unit ) {
      EventQueue.invokeLater( new Runnable { def run = code })
   }

//   def simulate( msg: OSCMessage ) { simulator.send( msg )}

   private def messageReceived( msg: OSCMessage, addr: SocketAddress, time: Long ) = msg match {
      case OSCMessage( "/cupola", "state", levelID: Int, sectionID: Int ) => levelChange( levelID, sectionID )
      case x => println( "Cupola: Ignoring OSC message '" + x + "'" )
   }

   private def levelChange( levelID: Int, sectionID: Int ) {
      val newLevel   = Level.all( levelID )
      val newSection = Section.all( sectionID )
      this ! LevelChanged( newLevel, newSection )
   }

   private def dispatch( msg: AnyRef ) {
      listeners.foreach( _ ! msg )
   }

   def act = loop {
      react {
         case msg: LevelChanged => {
            level    = msg.newLevel
            section  = msg.newSection
            dispatch( msg )
         }
         case QueryLevel      => () // reply( LevelChanged( level, section ))
//         case Run             => run
//         case Quit            => quit
         case AddListener     => listeners += sender
         case RemoveListener  => listeners -= sender 
         case x               => println( "Cupola: Ignoring actor message '" + x + "'" )
      }
   }

//   def run {
//      println( "Server booted. Starting Cupola..." )
////      (new ProcessManager).start
//      s.dumpOSC(1)
////      Test.run
//   }
//
//   def quit {
//      s.quit
//      s.dispose
////      tracking.dispose
//      System.exit( 0 )
//   }
}