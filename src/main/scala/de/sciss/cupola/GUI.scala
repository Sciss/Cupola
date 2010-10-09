/*
 *  GUI.scala
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

import Cupola._
import actors.Actor
import collection.breakOut
import de.sciss.osc.OSCMessage
import de.sciss.synth.proc.ProcTxn
import javax.swing._
import javax.swing.event.{MouseInputAdapter, ChangeEvent, ChangeListener}
import java.awt.event._
import java.awt._

/**
 *    @version 0.13, 09-Oct-10
 */
class GUI extends Cupola.Listener {
   gui =>

   private var valid = false

   private var selectedBut: StageButton = _
   private val ggIdle     = makeButton( IdleStage )
   private val ggCalib    = makeButton( CalibStage )
   private val ggHidden   = makeButton( HiddenStage )
   private val ggMedit    = makeButton( MeditStage )
   private val ggChaos    = makeButton( ChaosStage )
   private val ggEqui     = makeButton( EquiStage )
   private val ggLimbo    = makeButton( LimboStage )
   private val ggFinal    = makeButton( FinalStage )

   val ggLevel = new JComponent {
      private var scaleVar = 0.0
      def scale = scaleVar
      def scale_=( newVal: Double ) {
         if( newVal != scaleVar ) {
            scaleVar = newVal
            repaint()
         }
      }
      val adapter = new MouseInputAdapter {
         override def mousePressed( e: MouseEvent ) { adjust( e )}
         override def mouseDragged( e: MouseEvent ) { adjust( e )}
         def adjust( e: MouseEvent ) {
            scale = math.max( 0.0, math.min( 1.0, e.getX().toDouble / getWidth() ))
//            /* if( valid ) */ Cupola.simulate( OSCMessage( "/cupola", "state", scale.toFloat ))
            val stage = (scale * 8 + 0.5).toInt
            Cupola.simulate( OSCTrackingMessage( 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, stage ))
            Cupola.simulateLocal( OSCMessage( "/cupola", "state", scale.toFloat ))
         }
      }
      addMouseListener( adapter )
      addMouseMotionListener( adapter ) 
      setPreferredSize( new Dimension( 320, 32 ))

      override def paintComponent( g: Graphics ) {
         g.setColor( Color.black )
         g.fillRect( 0, 0, getWidth(), getHeight() )
         g.setColor( Color.white )
         g.drawRect( 0, 0, getWidth() - 1, getHeight() - 1 )
         val x = (scale * getWidth()).toInt
         g.fillRect( 0, 0, x, getHeight() )
      }
   }

   val ggDumpOSC = {
      val res = new JCheckBox()
      res.addActionListener( new ActionListener {
         def actionPerformed( e: ActionEvent ) {
            Cupola.simulate( OSCMessage( "/dumpOSC", if( res.isSelected ) 1 else 0 ))
         }
      })
      res.setFocusable( false )
      res.setToolTipText( "Dump relais OSC" )
      res
   }
   val ggDumpOSC2 = {
      val res = new JCheckBox()
      res.addActionListener( new ActionListener {
         def actionPerformed( e: ActionEvent ) {
            Cupola.dumpOSC( if( res.isSelected ) 1 else 0 )
         }
      })
      res.setFocusable( false )
      res.setToolTipText( "Dump local OSC" )
      res
   }
   val ggConnect = {
      val res = new JCheckBox()
      res.addActionListener( new ActionListener {
         def actionPerformed( e: ActionEvent ) {
            Cupola.trackingConnected = res.isSelected
         }
      })
      res.setFocusable( false )
      res.setSelected( Cupola.trackingConnected )
      res.setToolTipText( "Connect to relais" )
      res
   }

   // ---- constructor ----
   {
      val f    = new JFrame( "Cupola" )
      val cp   = f.getContentPane
      f.setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE )

      ProcTxn.atomic { implicit tx => Cupola.addListener( gui )}

      f.addWindowListener( new WindowAdapter {
         override def windowClosing( e: WindowEvent ) {
//            println( "SENDING QUIT" )
            Cupola.quit
         }
      })

      val yBox = Box.createVerticalBox()
      ggFinal.setAlignmentX( 0.5f )
      yBox.add( ggFinal )
      ggLimbo.setAlignmentX( 0.5f )
      yBox.add( ggLimbo )
      val xBox = Box.createHorizontalBox()
      xBox.add( ggMedit )
      xBox.add( ggEqui )
      xBox.add( ggChaos )
      xBox.setAlignmentX( 0.5f )
      yBox.add( xBox )
      ggHidden.setAlignmentX( 0.5f )
      yBox.add( ggHidden )
      ggCalib.setAlignmentX( 0.5f )
      yBox.add( ggCalib )
      ggIdle.setAlignmentX( 0.5f )
      yBox.add( ggIdle )

ggIdle.setBackground( Color.white )
ggIdle.setForeground( Color.black )


//      cp.add( levelPane, BorderLayout.CENTER )
//      cp.add( ggLevel, BorderLayout.CENTER )
      cp.add( yBox, BorderLayout.CENTER )
      val box = Box.createHorizontalBox()
      box.add( ggDumpOSC )
      box.add( ggDumpOSC2 )
      box.add( ggConnect )
      cp.add( box, BorderLayout.SOUTH )
      cp.setBackground( Color.black )
      f.setResizable( false )
      f.pack
      f.setLocation( 10, Cupola.SCREEN_BOUNDS.height - f.getHeight() - 10 )
      f.setVisible( true )
   }

   private def makeButton( stage: Stage ) : StageButton = {
      val b = new StageButton( stage )
      if( stage == IdleStage ) {
         b.selected  = true
         selectedBut = b
      }
      b.action = () => {
         val allowed = selectedBut.stage.transits.contains( stage )
//         println( "sel.stage = " + selectedBut.stage + " -> trans " + selectedBut.stage.transits + " :: " + stage + " ? " + allowed )
         if( allowed ) {
            selectedBut.selected = false
            b.selected = true
            selectedBut = b
         }
      }
      b
   }

   def updated( u: Cupola.Update ) {
      Cupola.guiRun {
         u.stage foreach { scale =>
            valid = true
//            val i = (scale * 0x1000).toInt
//            if( ggLevel.getValue() != i ) ggLevel.setValue( i )
            ggLevel.scale = scale
//            selectedCell.foreach( _.deselect )
//            selectedCell = map.get( tup )
//            selectedCell.foreach( _.select )
         }
      }
   }

//   private class Cell extends JComponent {
//      setPreferredSize( new Dimension( 64, 32 ))
//      setOpaque( false )
//      deselect
//
//      def select {
//         setBackground( Color.white )
//      }
//
//      def deselect {
//         setBackground( Color.black )
//      }
//
//      override def paintComponent( g: Graphics ) {
//         super.paintComponent( g )
//         g.setColor( getBackground )
//         g.fillRect( 1, 1, getWidth - 2, getHeight - 2 )
//      }
//   }

   class StageButton( val stage: Stage ) extends JLabel( stage.name.map( _.toUpper ), SwingConstants.CENTER ) {
      setFont( new Font( "SansSerif", Font.BOLD, 12 ))
      setBorder( BorderFactory.createCompoundBorder(
         BorderFactory.createEmptyBorder( 2, 2, 2, 2 ), BorderFactory.createCompoundBorder(
         BorderFactory.createMatteBorder( 2, 2, 2, 2, Color.white ), BorderFactory.createEmptyBorder( 4, 4, 4, 4 ))))

      private var selectedVar = true
      def selected = selectedVar
      def selected_=( onOff: Boolean ) {
         if( selectedVar != onOff ) {
            selectedVar = onOff
            setBackground( if( onOff ) Color.white else Color.black )
            setForeground( if( onOff ) Color.black else Color.white )
            repaint()
         }
      }

      var action = () => ()

      selected = false

      addMouseListener( new MouseAdapter {
         override def mousePressed( e: MouseEvent ) {
            if( !selected ) action()
         }
      })

      override def paintComponent( g: Graphics ) {
         g.setColor( getBackground() )
         g.fillRect( 0, 0, getWidth(), getHeight() )
         super.paintComponent( g )
      }
   }
}