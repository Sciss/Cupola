package de.sciss.cupola

//object Stage {
//   val all = Vector( Section1, Section2, Section3 )
//}
abstract sealed class Stage( val id: Int, val name: String ) { def transits: Set[ Stage ]}
case object IdleStage   extends Stage( 0, "idle" )  { val transits = Set[ Stage ]( CalibStage )}
case object CalibStage  extends Stage( 1, "calib" ) { val transits = Set[ Stage ]( IdleStage, HiddenStage )}
case object HiddenStage extends Stage( 2, "hidden" ){ val transits = Set[ Stage ]( IdleStage, MeditStage, ChaosStage )}
case object MeditStage  extends Stage( 3, "medit" ) { val transits = Set[ Stage ]( IdleStage, EquiStage )}
case object EquiStage   extends Stage( 4, "equi" )  { val transits = Set[ Stage ]( IdleStage, LimboStage, MeditStage, ChaosStage )}
case object ChaosStage  extends Stage( 5, "chaos" ) { val transits = Set[ Stage ]( IdleStage, EquiStage )}
case object LimboStage  extends Stage( 6, "limbo" ) { val transits = Set[ Stage ]( IdleStage, FinalStage )}
case object FinalStage  extends Stage( 7, "final" ) { val transits = Set[ Stage ]( IdleStage )}
