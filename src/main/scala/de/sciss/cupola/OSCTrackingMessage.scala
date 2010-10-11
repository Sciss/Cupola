package de.sciss.cupola

import java.nio.ByteBuffer
import de.sciss.osc.{OSCPacket, OSCException, OSCPacketCodec, OSCMessage}

object OSCTrackingMessage {
//   val empty = OSCTrackingMessage( 50f, 50f, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 )
   val empty = OSCTrackingMessage( 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 )
}
//case class OSCTrackingMessage( pointX: Float, pointY: Float, eyeLX: Int, eyeLY: Int, eyeRX: Int, eyeRY: Int,
//                            pupLX: Int, pupLY: Int, pupRX: Int, pupRY: Int, blinkL: Int, blinkR: Int, state: Int )
case class OSCTrackingMessage( pointX: Int, pointY: Int, eyeLX: Int, eyeLY: Int, eyeRX: Int, eyeRY: Int,
                            pupLX: Int, pupLY: Int, pupRX: Int, pupRY: Int, blinkL: Int, blinkR: Int, state: Int )
extends OSCMessage( "/t", pointX, pointY, eyeLX, eyeLY, eyeRX, eyeRY, pupLX, pupLY, pupRX, pupRY, blinkL, blinkR, state )

case class OSCStageMessage( id: Int ) extends OSCMessage( "/stage", id )
case class OSCDistMessage( value: Float ) extends OSCMessage( "/dist", value )
case class OSCTrigMessage( id: Int ) extends OSCMessage( "/trig", id )

object OSCTrackingCodec extends OSCPacketCodec {
   private def decodeStage( b: ByteBuffer ) : OSCMessage = {
      // ",i"
      if( (b.getShort() != 0x2C69) ) decodeFail
		OSCPacket.skipToValues( b )
      val id = b.getInt()
      OSCStageMessage( id )
   }

   private def decodeDist( b: ByteBuffer ) : OSCMessage = {
      // ",f"
      if( (b.getShort() != 0x2C66) ) decodeFail
      OSCPacket.skipToValues( b )
      val value = b.getFloat()
      OSCDistMessage( value )
   }

   private def decodeTrig( b: ByteBuffer ) : OSCMessage = {
      // ",i"
      if( (b.getShort() != 0x2C69) ) decodeFail
		OSCPacket.skipToValues( b )
      val id = b.getInt()
      OSCTrigMessage( id )
   }

   private def decodeTracking( b: ByteBuffer ) : OSCMessage = {
//      // ",ffiiiii iiiiii"
//      if( (b.getLong() != 0x2C66666969696969L) || (b.getInt() != 0x69696969) || (b.getShort() != 0x6969) ) decodeFail
         // ",ffiiiii iiiiii"
         if( (b.getLong() != 0x2C69696969696969L) || (b.getInt() != 0x69696969) || (b.getShort() != 0x6969) ) decodeFail
         OSCPacket.skipToValues( b )

//      val pointX        = b.getFloat()
//      val pointY        = b.getFloat()
         val pointX        = b.getInt()
         val pointY        = b.getInt()
         val eyeLX         = b.getInt()
         val eyeLY         = b.getInt()
         val eyeRX         = b.getInt()
         val eyeRY         = b.getInt()
         val pupLX         = b.getInt()
         val pupLY         = b.getInt()
         val pupRX         = b.getInt()
         val pupRY         = b.getInt()
         val blinkL        = b.getInt()
         val blinkR        = b.getInt()
         val state         = b.getInt()

		OSCTrackingMessage( pointX, pointY, eyeLX, eyeLY, eyeRX, eyeRY, pupLX, pupLY, pupRX, pupRY, blinkL, blinkR, state )
	}

   override protected def decodeMessage( name: String, b: ByteBuffer ) : OSCMessage = {
      name match {
         case "/dist"   => decodeDist( b )
         case "/trig"   => decodeTrig( b )
         case "/stage"  => decodeStage( b )
         case "/t"      => decodeTracking( b )
         case _         => super.decodeMessage( name, b )
      }
	}

   private def decodeFail : Nothing = throw new OSCException( OSCException.DECODE, null )
}
