/*
 *  GUI.scala
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

import collection.breakOut
import de.sciss.synth.proc.ProcTxn
import javax.swing._
import javax.swing.event.MouseInputAdapter
import java.awt.event._
import java.awt._
import de.sciss.osc

class GUI extends Cupola.Listener {
   gui =>

   private var selectedBut: Option[ StageButton ] = None
   private val ggIdle     = makeButton( IdleStage )
   private val ggCalib    = makeButton( CalibStage )
   private val ggHidden   = makeButton( HiddenStage )
   private val ggMedit    = makeButton( MeditStage )
   private val ggChaos    = makeButton( ChaosStage )
   private val ggEqui     = makeButton( EquiStage )
   private val ggLimbo    = makeButton( LimboStage )
   private val ggFinal    = makeButton( FinalStage )
   private val ggMap: Map[ Stage, StageButton ] =
      Seq( ggIdle, ggCalib, ggHidden, ggMedit, ggChaos, ggEqui, ggLimbo, ggFinal ).map( but => but.stage -> but)( breakOut )

   val ggLevel = {
      val res = new Slider
      res.action = lvl => {
//println( "DIST = " + lvl )
         Cupola.simulateBoth( OSCDistMessage( lvl.toFloat ))
      }
      res
   }
   val ggDumpOSC = {
      val res = new JCheckBox()
      res.addActionListener( new ActionListener {
         def actionPerformed( e: ActionEvent ) {
            Cupola.simulateRemote( osc.Message( "/dumpOSC", if( res.isSelected ) 1 else 0 ))
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
            Cupola.dumpOSC( if( res.isSelected ) osc.Dump.Text else osc.Dump.Off )
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
            Cupola.quit()
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
      cp.add( yBox, BorderLayout.CENTER )

      val eBox = Box.createVerticalBox()
      eBox.add( ggLevel )
      cp.add( eBox, BorderLayout.EAST )

      val box = Box.createHorizontalBox()
      box.setBorder( BorderFactory.createEmptyBorder( 4, 2, 2, 2 ))
      box.add( ggDumpOSC )
      box.add( ggDumpOSC2 )
      box.add( ggConnect )
      cp.add( box, BorderLayout.SOUTH )
      cp.setBackground( Color.black )
      f.setResizable( false )
      f.pack()
      f.setLocation( 10, Cupola.SCREEN_BOUNDS.height - f.getHeight - 10 )
      f.setVisible( true )
   }

   private def makeButton( stage: Stage ) : StageButton = {
      val b = new StageButton( stage )
      b.action = () => {
         val allowed = selectedBut.map( _.stage.transits.contains( stage )).getOrElse( false )
         if( allowed ) {
            Cupola.simulateBoth( OSCStageMessage( stage.id, 0f ))
         }
      }
      b
   }

   def updated( u: Cupola.Update ) {
      Cupola.guiRun {
         u.stage foreach { stage =>
            selectedBut.foreach( _.selected = false )
            ggMap.get( stage ) foreach { but =>
               selectedBut = Some( but )
               but.selected = true
            }
         }
         u.dist.foreach( ggLevel.value = _ )
      }
   }

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
         g.setColor( getBackground )
         g.fillRect( 0, 0, getWidth, getHeight )
         super.paintComponent( g )
      }
   }

   class Slider extends JComponent {
      var action = (d: Double) => ()

      private var valueVar = 0.0
      def value = valueVar
      def value_=( newVal: Double ) {
         if( newVal != valueVar ) {
            valueVar = newVal
            repaint()
         }
      }
      private val adapter = new MouseInputAdapter {
         override def mousePressed( e: MouseEvent ) { adjust( e )}
         override def mouseDragged( e: MouseEvent ) { adjust( e )}
         def adjust( e: MouseEvent ) {
            val i = getInsets
            val v = math.max( 0.0, math.min( 1.0, (e.getX - i.left).toDouble / (getWidth - (i.left + i.right)) ))
            action( v )
         }
      }
      addMouseListener( adapter )
      addMouseMotionListener( adapter )
      setPreferredSize( new Dimension( 320, 32 ))
      setBorder( BorderFactory.createCompoundBorder(
         BorderFactory.createEmptyBorder( 2, 2, 2, 2 ), BorderFactory.createCompoundBorder(
         BorderFactory.createMatteBorder( 2, 2, 2, 2, Color.white ), BorderFactory.createEmptyBorder( 2, 2, 2, 2 ))))
      setBackground( Color.black )
      setForeground( Color.white )

      override def paintComponent( g: Graphics ) {
         g.setColor( getBackground )
         g.fillRect( 0, 0, getWidth, getHeight )
         g.setColor( getForeground )
         val i = getInsets
         g.fillRect( i.left, i.top, ((getWidth - (i.left + i.right)) * valueVar).toInt, getHeight - (i.top + i.bottom) )
//         g.setColor( Color.white )
//         g.drawRect( 0, 0, getWidth() - 1, getHeight() - 1 )
//         val x = (scale * getWidth()).toInt
//         g.fillRect( 0, 0, x, getHeight() )
      }
   }
}