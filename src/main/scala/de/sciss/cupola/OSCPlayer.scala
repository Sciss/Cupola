package de.sciss.cupola

import java.nio.ByteBuffer
import java.util.{Timer, TimerTask}
import java.io.{EOFException, RandomAccessFile, File}
import sys.error
import de.sciss.osc

class OSCPlayer( file: File, codec: osc.PacketCodec ) {
   player =>

   private val raf   = new RandomAccessFile( file, "r" )
   private val ch    = raf.getChannel
   private val bb    = ByteBuffer.allocate( 8192 )
   private val sync  = new AnyRef
   private val timer = new Timer()

   var action: osc.Packet => Unit = p => ()

   private var nextBundle: osc.Bundle = null
   private var startSysTime = 0L
   private var startBndlTime = 0L

//   def add( p: OSCPacket ) {
//      sync.synchronized {
//         bb.clear()
//         p.encode( codec, bb )
//         bb.flip()
//         raf.writeInt( bb.limit() )
//         ch.write( bb )
//      }
//   }

   def start() {
      sync.synchronized {
         raf.seek( 0L )
         nextBundle = readBundle
         startBndlTime  = nextBundle.timetag.toMillis // osc.Bundle.timetagToMillis( nextBundle.timetag )
         startSysTime   = System.currentTimeMillis
         sched( 0L )
      }
   }

   def stop() {
      sync.synchronized { timer.cancel() }
   }

   private def sched( dt: Long ) {
//println( "DT = " + dt )
      timer.schedule( new TimerTask {
         def run() { sync.synchronized {
            try {
               var delta = 0L
               do {
                  action( nextBundle )
                  nextBundle = readBundle // may throw EOF
//println( "nextBundle = " + nextBundle )
                  val dtBndl = nextBundle.timetag.toMillis - startBndlTime // osc.Bundle.timetagToMillis( nextBundle.timetag ) - startBndlTime
                  val dtSys  = System.currentTimeMillis - startSysTime
                  delta = math.max( 0L, dtBndl - dtSys )
//                  delta = math.max( 0L, OSCBundle.timetagToMillis( nextBundle.timetag ) - startTime )
               } while( delta == 0L )
               sched( delta )
            }
            catch {
               case eof: EOFException => // stop
               case e => e.printStackTrace()
            }
         }}
      }, dt )
   }

   private def readBundle : osc.Bundle = {
      val size = raf.readInt()
      bb.rewind().limit( size )
      ch.read( bb )
      bb.flip()
      codec.decode( bb ) match {
         case b: osc.Bundle => b
         case _ => error( "Expecting OSC bundles" )
      }
   }

   def close() {
      sync.synchronized { stop(); raf.close() }
   }
}