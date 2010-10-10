package de.sciss.cupola

import de.sciss.synth.proc.ProcTxn
import CupolaNuages._

class ProcessManager2 {
   def stageChange( oldStage: Stage, newStage: Stage )( implicit tx: ProcTxn ) {
      procMap.get( oldStage ).foreach( _.active = false )
      procMap.get( newStage ).foreach( _.active = true )
   }
}