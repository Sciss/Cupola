/*
 *  ProcessManager2.scala
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

import de.sciss.synth.proc.ProcTxn
import CupolaNuages._

class ProcessManager2 {
   def stageChange( oldStage: Stage, newStage: Stage )( implicit tx: ProcTxn ) {
      procMap.get( oldStage ).foreach( _.active = false )
      procMap.get( newStage ).foreach( _.active = true )
   }

   def distChange( newDist: Double )( implicit tx: ProcTxn ) {
      procMap.collect({ case (_, p: CupolaDistProcess) => p }).foreach( _.distChanged( newDist ))
   }
}