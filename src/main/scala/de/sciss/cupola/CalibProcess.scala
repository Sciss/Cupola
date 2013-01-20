/*
 *  CalibProcess.scala
 *  (Cupola)
 *
 *  Copyright (c) 2010-2013 Hanns Holger Rutz. All rights reserved.
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

import de.sciss.synth._
import de.sciss.synth.ugen._
import de.sciss.synth.proc._
import Cupola._
import CupolaNuages._
import DSL._

object CalibProcess {
//  private def any2stringadd(x: Any) {}  // fucking shit

//   val FADEIN  = 10.0
   val FADEOUT = 10.0
   val GAIN    = -3.0
   val GESTURE_FADEOUT  = 4.0
   val GESTURE_DUR = 15.0

   val spans = Vector( 40883 -> 1836046, 2233632 -> 3400591, 3749675 -> 5387433, 5443508 -> 7090700,
      7144479 -> 8625153, 8665193 -> 10151294 )
   val gains = Vector( 8 -> 1, 1 -> 1, 1 -> 1, 8 -> 1, 32 -> 1, 1 -> 2 )
   val pan0  = 0.0
   val pans  = Vector( 1, -1, 1, -1 ) // per gesture
   val urn = new Urn( (0 until spans.size): _* )
   val NUM_GESTURES = 4
   
   private case class Active( pgen: Proc, count: Int )
}

class CalibProcess extends CupolaProcess {
   import CalibProcess._
   
   private val activeRef: Ref[ Option[ Active ]] = Ref( None )

   def active( implicit tx: ProcTxn ) : Boolean = activeRef().isDefined
   def active_=( onOff: Boolean )( implicit tx: ProcTxn ) {
      val wasActive = activeRef()
      if( wasActive.isDefined == onOff ) return
      wasActive foreach { act =>
         ProcHelper.stopAndDispose( FADEOUT, act.pgen )
      }
      activeRef.set( if( onOff ) Some( start ) else None )
      if( onOff ) nextGesture
   }

   def name = "calib"

   private def nextGesture( implicit tx: ProcTxn ) { activeRef() foreach { oldAct =>
      val p = oldAct.pgen
      xfade( GESTURE_FADEOUT ) { p.stop }
      if( oldAct.count > 0 ) {
         val newAct = oldAct.copy( count = oldAct.count - 1 )
         activeRef.set( Some( newAct ))
         val nextID = urn.next
         val span   = spans( nextID )
         val gain   = gains( nextID )
         p.control( "start" ).v = span._1
         p.control( "dur" ).v = GESTURE_DUR // (span._2 - span._1).toDouble / (44100 * 2)
         p.control( "gain-l" ).v = gain._1
         p.control( "gain-r" ).v = gain._2
         xfade( 0.1 ) { p.play }
         glide( GESTURE_DUR ) { p.control( "bal" ).v = pans( pans.size - oldAct.count )}
      }
   }}

   private def start( implicit tx: ProcTxn ) : Active = {
      val g = (ProcDemiurg.factories.find( _.name == name ) getOrElse gen( name ) {
         val pamp = pAudio( "amp", ParamSpec( 0.001, 10, ExpWarp ), GAIN.dbamp )
         val pstart = pScalar( "start", ParamSpec( 0, 10151294 ), 0 )
         val pdur   = pScalar( "dur", ParamSpec( 1, 20 ), 1 )
         /* val pfire  = */ pScalar( "fire", ParamSpec( 1, 20 ), 1 )
         val pgainl = pScalar( "gain-l", ParamSpec( 0.01, 100, ExpWarp ), 1 )
         val pgainr = pScalar( "gain-r", ParamSpec( 0.01, 100, ExpWarp ), 1 )
         val pspeed = pControl( "speed", ParamSpec( 0.1, 10, ExpWarp ), 1 )
         val pbal   = pAudio( "bal", ParamSpec( -1, 1 ), pan0 )
         graph {
            val buf = bufCue( AUDIO_PATH + fs + "GlssD'ynth5ConGlssD'ynth6FTOpAmpOpFTHlbSt.aif", pstart.v.toLong )
            val sig = VDiskIn.ar( buf.numChannels, buf.id, pspeed.kr )
            val done = Done.kr( Line.kr( 0, 0, pdur.ir ))
            done.react {
               ProcTxn.spawnAtomic { implicit tx => nextGesture }
            }
            val sigL = (sig \ 0) * pgainl.ir
            val sigR = (sig \ 1) * pgainr.ir
            val bal  = pbal.ar
            val mulL = (1 - bal).min( 1 ).squared
            val mulR = (Constant(1) + bal).min( 1 ).squared // fucking Predef.any2stringadd
            (((sigL * mulL) :: (sigR * mulR) :: Nil): GE) * pamp.ar
         }

//         graph {
//            val path = AUDIO_PATH + fs + "2A-SideBlossCon2A-SideBloss.aif"
//            val buf  = bufCue( path, 8 * 44100 )
//            val sig  = DiskIn.ar( buf.numChannels, buf.id )
//            val done = Done.kr( Line.kr( 0, 0, 8 * 60 ))
//            done.react {
//               ProcTxn.spawnAtomic { implicit tx =>
//                  activeRef() foreach { act =>
//                     xfade( 20 ) { act.pgen.stop; act.pgen.play }
//                  }
//               }
//            }
//            sig * pamp.ar
//         }
      }).make

      g ~> collMaster
//      xfade( FADEIN ) { f.play }

      Active( g, NUM_GESTURES )
   }
}