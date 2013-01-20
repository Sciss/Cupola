package de.sciss.cupola

import java.nio.ByteBuffer
import de.sciss.osc
import osc.{Bundle, Message}
import java.io.PrintStream

object OSCTrackingMessage {
//   val empty = OSCTrackingMessage( 50f, 50f, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 )
   val empty = OSCTrackingMessage( 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 )
}
//case class OSCTrackingMessage( pointX: Float, pointY: Float, eyeLX: Int, eyeLY: Int, eyeRX: Int, eyeRY: Int,
//                            pupLX: Int, pupLY: Int, pupRX: Int, pupRY: Int, blinkL: Int, blinkR: Int, state: Int )
case class OSCTrackingMessage( pointX: Int, pointY: Int, eyeLX: Int, eyeLY: Int, eyeRX: Int, eyeRY: Int,
                            pupLX: Int, pupLY: Int, pupRX: Int, pupRY: Int, blinkL: Int, blinkR: Int, state: Int )
extends osc.Message( "/t", pointX, pointY, eyeLX, eyeLY, eyeRX, eyeRY, pupLX, pupLY, pupRX, pupRY, blinkL, blinkR, state )

case class OSCStageMessage( id: Int, _dunno: Float ) extends osc.Message( "/stage_data", id, _dunno )
case class OSCDistMessage( value: Float ) extends osc.Message( "/dist", value )
case class OSCTrigMessage( id: Int ) extends osc.Message( "/trig", id )

object OSCTrackingCodec extends osc.PacketCodec {
   final val fallback = osc.PacketCodec.default


   final val charsetName: String = fallback.charsetName
   def printAtom( value: Any, stream: PrintStream, nestCount: Int ) { fallback.printAtom( value, stream, nestCount )}
   def encodeBundle( bndl: Bundle, b: ByteBuffer ) { fallback.encodeBundle( bndl, b )}
   def encodeMessage( msg: Message, b: ByteBuffer ) { fallback.encodeMessage( msg, b )}
   def encodedMessageSize( msg: Message ) : Int = fallback.encodedMessageSize( msg )

   private def decodeStage( b: ByteBuffer ) : osc.Message = {
      // ",if"
//      if( (b.getShort != 0x2C69) ) decodeFail
      if( (b.getInt != 0x2C696600) ) decodeFail( "/stage_data" )
//		OSCPacket.skipToValues( b )
      val id      = b.getInt
      val _dunno  = b.getFloat
      OSCStageMessage( id, _dunno )
   }

   private def decodeDist( b: ByteBuffer ) : osc.Message = {
      // ",f"
      if( (b.getShort != 0x2C66) ) decodeFail( "/dist" )
      osc.Packet.skipToValues( b )
      val value = b.getFloat
      OSCDistMessage( value )
   }

   private def decodeTrig( b: ByteBuffer ) : osc.Message = {
      // ",i"
      if( (b.getShort != 0x2C69) ) decodeFail( "/trig" )
      osc.Packet.skipToValues( b )
      val id = b.getInt
      OSCTrigMessage( id )
   }

   private def decodeTracking( b: ByteBuffer ) : osc.Message = {
//      // ",ffiiiii iiiiii"
//      if( (b.getLong() != 0x2C66666969696969L) || (b.getInt() != 0x69696969) || (b.getShort() != 0x6969) ) decodeFail
         // ",ffiiiii iiiiii"
         if( (b.getLong != 0x2C69696969696969L) || (b.getInt != 0x69696969) || (b.getShort != 0x6969) ) decodeFail( "/t" )
         osc.Packet.skipToValues( b )

//      val pointX        = b.getFloat()
//      val pointY        = b.getFloat()
         val pointX        = b.getInt
         val pointY        = b.getInt
         val eyeLX         = b.getInt
         val eyeLY         = b.getInt
         val eyeRX         = b.getInt
         val eyeRY         = b.getInt
         val pupLX         = b.getInt
         val pupLY         = b.getInt
         val pupRX         = b.getInt
         val pupRY         = b.getInt
         val blinkL        = b.getInt
         val blinkR        = b.getInt
         val state         = b.getInt

		OSCTrackingMessage( pointX, pointY, eyeLX, eyeLY, eyeRX, eyeRY, pupLX, pupLY, pupRX, pupRY, blinkL, blinkR, state )
	}

   /* override protected */ def decodeMessage( name: String, b: ByteBuffer ) : osc.Message = {
      name match {
         case "/dist"   => decodeDist( b )
         case "/trig"   => decodeTrig( b )
         case "/stage_data"  => decodeStage( b )
         case "/t"      => decodeTracking( b )
         case _         => fallback.decodeMessage( name, b ) // super.decodeMessage( name, b )
      }
	}

   private def decodeFail( name: String ) : Nothing = throw osc.PacketCodec.MalformedPacket( name )
}
