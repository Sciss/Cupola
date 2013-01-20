//package de.sciss.cupola
//
//import de.sciss.scalainterpreter.LogPane
//import java.io.PrintStream
//import java.awt.event.KeyEvent
//import java.awt.{Toolkit, GraphicsEnvironment}
//import javax.swing._
//import tools.nsc.interpreter.NamedParam
//
//class ScalaInterpreterFrame( support: REPLSupport /* s: Server, ntp: NodeTreePanel*/ )
//extends JFrame( "Scala Interpreter" ) {
//   val pane = new ScalaInterpreterPane
////   private val sync = new AnyRef
////   private var inCode: Option[ Interpreter => Unit ] = None
////   private var interpreter: Option[ Interpreter ] = None
//
//   private val txnKeyStroke = {
//      val ms = Toolkit.getDefaultToolkit.getMenuShortcutKeyMask
//      KeyStroke.getKeyStroke( KeyEvent.VK_T, ms )
//   }
//
//   // ---- constructor ----
//   {
//      val cp = getContentPane
//
//      pane.initialText = pane.initialText +
//"""// Press '""" + KeyEvent.getKeyModifiersText( txnKeyStroke.getModifiers ) + " + " +
//      KeyEvent.getKeyText( txnKeyStroke.getKeyCode ) + """' to execute transactionally.
//
//"""
//
//      pane.initialCode = Some(
//"""
//import de.sciss.synth._
//import de.sciss.synth.ugen._
//import de.sciss.synth.swing._
//import de.sciss.synth.proc._
//import de.sciss.synth.proc.DSL._
//import support._
//import de.sciss.cupola._
//import Cupola._
//"""
//      )
//
//      pane.customBindings = Seq( NamedParam( "support", support ))
//
//      val lp = new LogPane
//      lp.init()
//      pane.out = Some( lp.writer )
//      Console.setOut( lp.outputStream )
//      Console.setErr( lp.outputStream )
//      System.setErr( new PrintStream( lp.outputStream ))
//
//      pane.customKeyMapActions += txnKeyStroke -> (() => txnExecute())
//
//      pane.init()
//      val sp = new JSplitPane( SwingConstants.HORIZONTAL )
//      sp.setTopComponent( pane )
//      sp.setBottomComponent( lp )
//      cp.add( sp )
//      val b = GraphicsEnvironment.getLocalGraphicsEnvironment.getMaximumWindowBounds
//      setSize( b.width / 2, b.height * 7 / 8 )
//      sp.setDividerLocation( b.height * 2 / 3 )
//      setLocationRelativeTo( null )
////    setLocation( x, getY )
//      setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE )
////    setVisible( true )
//   }
//
//   private var txnCount = 0
//
//   def txnExecute() {
//      pane.getSelectedTextOrCurrentLine.foreach( txt => {
//         val txnId  = txnCount
//         txnCount += 1
//         val txnTxt = """class _txnBody""" + txnId + """( implicit t: ProcTxn ) {
//""" + txt + """
//}
//val _txnRes""" + txnId + """ = ProcTxn.atomic( implicit t => new _txnBody""" + txnId + """ )
//import _txnRes""" + txnId + """._
//"""
//
////         println( txnTxt )
//         pane.interpret( txnTxt )
//      })
//   }
//
////   def withInterpreter( fun: Interpreter => Unit ) {
////      sync.synchronized {
////println( "withInterpreter " + interpreter.isDefined )
////         interpreter.map( fun( _ )) getOrElse {
////            inCode = Some( fun )
////         }
////      }
////   }
//}