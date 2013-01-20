/*
 *  IdleProcess.scala
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

object IdleProcess {
   val FADEIN  = 10.0
   val FADEOUT = 10.0
   val GAIN    = -4.5

   private case class Active( pgen: Proc, pfilter: Proc )
}

class IdleProcess extends CupolaProcess {
   import IdleProcess._
   
   private val activeRef: Ref[ Option[ Active ]] = Ref( None )

   def active( implicit tx: ProcTxn ) : Boolean = activeRef().isDefined
   def active_=( onOff: Boolean )( implicit tx: ProcTxn ) {
      val wasActive = activeRef()
      if( wasActive.isDefined == onOff ) return
      wasActive foreach { act =>
         ProcHelper.stopAndDispose( FADEOUT, act.pgen )
         ProcHelper.stopAndDispose( FADEOUT, act.pfilter )
      }
      activeRef.set( if( onOff ) Some( start ) else None )
   }

   def name = "idle"

   private def start( implicit tx: ProcTxn ) : Active = {
      val g = (ProcDemiurg.factories.find( _.name == name ) getOrElse gen( name ) {
         val pamp = pAudio( "amp", ParamSpec( 0.001, 10, ExpWarp ), GAIN.dbamp )
         graph {
            val path = AUDIO_PATH + fs + "2A-SideBlossCon2A-SideBloss.aif"
            val buf  = bufCue( path, 8 * 44100 )
            val sig  = DiskIn.ar( buf.numChannels, buf.id )
            val done = Done.kr( Line.kr( 0, 0, 8 * 60 ))
            done.react {
               ProcTxn.spawnAtomic { implicit tx =>
                  activeRef() foreach { act =>
                     xfade( 20 ) { act.pgen.stop; act.pgen.play }
                  }
               }
            }
            sig * pamp.ar
         }
      }).make

      val fname =  name + "-flt"
      val f = (ProcDemiurg.factories.find( _.name == fname ) getOrElse filter( fname ) {
         graph { in: In =>
            val freq = 1.0 // pfreq.kr
            val tr  = Impulse.kr( freq )
            val trs = PulseDivider.kr( tr, 2, 1 :: 0 :: Nil )
            val fs = TRand.kr( 0, 1, trs ).squared.linexp( 0, 1, 80, 8000 )
            val rq  = 30.reciprocal
            val gain = 8 // pgain.kr // 6
            val eqs = Resonz.ar( in, fs, rq ) * gain
            val ff1 = ToggleFF.kr( TDelay.kr( tr, freq * 0.1 ))
            val ffs: GE = ff1 :: (1 - ff1) :: Nil
            eqs * LagUD.kr( ffs, 0.2, 4 ) + in
         }
      }).make

      g ~> f
      f ~> collMaster
      xfade( FADEIN ) { f.play }

      Active( g, f )
   }
}