package de.sciss.cupola

import java.io.{RandomAccessFile, File}
import java.nio.ByteBuffer
import de.sciss.osc

class OSCRecorder( file: File, codec: osc.PacketCodec ) {
   private val raf   = new RandomAccessFile( file, "rw" )
   private val ch    = raf.getChannel
   private val bb    = ByteBuffer.allocate( 8192 )
   private val sync  = new AnyRef

   def add( p: osc.Bundle ) {
      sync.synchronized {
         bb.clear()
         p.encode( codec, bb )
         bb.flip()
         raf.writeInt( bb.limit() )
         ch.write( bb )
      }
   }

   def close() {
      sync.synchronized { raf.close() }
   }
}