/*
 *  Proc.scala
 *  (ScalaCollider-Proc)
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

package de.sciss.synth.proc

import collection.immutable.{ IndexedSeq => IIdxSeq, Seq => ISeq }
import de.sciss.scalaosc.OSCMessage
import de.sciss.synth.{Model, AudioBus, Group, Server}

/**
 *    @version 0.12, 29-Jun-10
 *
 *    @todo XXX after switching to using an actor
 *          to represent a proc, we should get rid
 *          of the thread-local variable, and replace
 *          occurences of Proc.local with Actor.self 
 */
object Proc extends ThreadLocalObject[ Proc ] {
   case class PlayingChanged( proc: Proc, playing: Boolean )
   case class ControlsChanged( controls: (ProcControl, Float)* )
   case class MappingsChanged( controls: (ProcControl, Option[ ProcControlMapping ])* )
   case class AudioBusesConnected( edges: ProcEdge* )
   case class AudioBusesDisconnected( edges: ProcEdge* )
}

trait Proc extends Model {
   def name : String
   def play( implicit tx: ProcTxn ) : Proc
   def stop( implicit tx: ProcTxn ) : Proc
   def isPlaying( implicit tx: ProcTxn ) : Boolean
   def server : Server

//   def getFloat( name: String )( implicit tx: ProcTxn ) : Float
//   def setFloat( name: String, value: Float )( implicit tx: ProcTxn ) : Proc
   def getString( name: String )( implicit tx: ProcTxn ) : String
   def setString( name: String, value: String )( implicit tx: ProcTxn ) : Proc

// for now disabled:
//   def getAudioBus( name: String )( implicit tx: ProcTxn ) : RichAudioBus
//   def setAudioBus( name: String, value: RichAudioBus )( implicit tx: ProcTxn ) : Proc

   def params : IIdxSeq[ ProcParam ]
   def param( name: String ) : ProcParam
   def controls : IIdxSeq[ ProcControl ]
   def control( name: String ) : ProcControl

   def audioInputs : IIdxSeq[ ProcAudioInput ]
   def audioInput( name: String ) : ProcAudioInput
   def audioOutputs : IIdxSeq[ ProcAudioOutput ]
   def audioOutput( name: String ) : ProcAudioOutput

   /**
    *    Retrieves the main group of the Proc, or
    *    returns None if a group has not yet been assigned.
    */
   def groupOption( implicit tx: ProcTxn ) : Option[ RichGroup ]

   /**
    *    Retrieves the main group of the Proc. If this
    *    group has not been assigned yet, this method will
    *    create a new group.
    */
   def group( implicit tx: ProcTxn ) : RichGroup

   /**
    *    Assigns a group to the Proc.
    */
   def group_=( newGroup: RichGroup )( implicit tx: ProcTxn ) : Unit

   def playGroupOption( implicit tx: ProcTxn ) : Option[ RichGroup ]
   def playGroup( implicit tx: ProcTxn ) : RichGroup
   def playGroup_=( newGroup: RichGroup )( implicit tx: ProcTxn )

//   private[proc] def connect( out: ProcAudioOutput, in: ProcAudioInput )( implicit tx: ProcTxn ) : Unit
//   private[proc] def disconnect( out: ProcAudioOutput, in: ProcAudioInput ) : Unit
//   private[proc] def insert( out: ProcAudioOutput, in: ProcAudioInput, insert: (ProcAudioInput, ProcAudioOutput) ) : Unit
}

case class ProcEdge( out: ProcAudioOutput, in: ProcAudioInput )
extends Topology.Edge[ Proc ] {
   def sourceVertex = out.proc
   def targetVertex = in.proc
}