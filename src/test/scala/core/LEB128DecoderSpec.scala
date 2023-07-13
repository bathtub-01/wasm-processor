package core

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

import scala.util.Random

object LEB128DecoderSimulator {

  def unsignedLEB128Gen(in: Int = -1): (BigInt, List[Byte]) = {
    val n = BigInt(if(in == -1) Random.nextInt(Int.MaxValue) else in)
    def go(n: BigInt, acc: List[Byte]): List[Byte] = {
      if (n == 0) acc
      else {
        val byte = ((n & 0x7f) | (if((n >> 7) != 0) 0x80 else 0)).toByte
        go((n >> 7), byte :: acc)  
      }
    }
    (n, go(n, Nil).reverse)
  }

  def signedLEB128Gen(in: Int = 0): (Int, List[Byte]) = {
    val n = if(in == 0) Random.nextInt() else in
    def go(n: Int, acc: List[Byte]): List[Byte] = {
      if (n == 0) acc
      else {
        val byte = ((n & 0x7f) | (if((n >> 7) != 0) 0x80 else 0)).toByte
        go((n >>> 7), byte :: acc)  
      }
    }
    (n, go(n, Nil).reverse)
  }

  def unsignedDecode(in: List[Byte]): BigInt = {
    in.map(byte => BigInt(byte & 0x7f)).zip(0 to 5)
      .map{case (byte, shift) => byte << (shift * 7)}.reduce(_ | _)
  }

  def signedDecode(in: List[Byte]): Int = {
    in.map(byte => byte & 0x7f).zip(0 to 5)
      .map{case (byte, shift) => byte << (shift * 7)}.reduce(_ | _)
  }
}

class LEB128DecoderSpec extends AnyFreeSpec with ChiselScalatestTester {
  import LEB128DecoderSimulator._
  def pendList(in: List[Byte]): List[Byte] = if(in.length < 5) pendList(in :+ 0) else in

  "Unsigned LEB128 Decoder should decode" in {
    test(new LEB128UnsignedDecoder)/*.withAnnotations(Seq(WriteVcdAnnotation))*/ { dut =>
      def run(n: Int, nums: List[Int] = Nil): Unit = {
        val (num, leb128) = unsignedLEB128Gen(if(nums.isEmpty) -1 else nums.head)
        val out = unsignedDecode(pendList(leb128))
        println(s"BigInt: $num, LEB128: $leb128")
        println(s"simulator | out(BigInt): $out")
        pendList(leb128).zip(0 to 5).foreach{case (byte, i) =>
          dut.io.din(i).poke(byte.toInt & 0xff)
        }
        dut.io.dout.expect(out)
        println(s"hardware  | out: ${dut.io.dout.peekInt()}")
        println("pass.")
        println("======================================================")
        dut.clock.step()
        if(n > 1) {
          run(n - 1, if(nums.isEmpty) Nil else nums.tail)
        }
      }

      dut.clock.step(3)
      run(100)
      // run(4, List(0, 1, 8, 624485))
    }
  }

  "Signed LEB128 Decoder should decode" in {
    test(new LEB128SignedDecoder)/*.withAnnotations(Seq(WriteVcdAnnotation))*/ { dut =>
      def run(n: Int, nums: List[Int] = Nil): Unit = {
        val (num, leb128) = signedLEB128Gen(if(nums.isEmpty) 0 else nums.head)
        val out = signedDecode(pendList(leb128))
        println(s"Int: $num, LEB128: $leb128")
        println(s"simulator | out: $out")
        pendList(leb128).zip(0 to 5).foreach{case (byte, i) =>
          dut.io.din(i).poke(byte.toInt & 0xff)
        }
        dut.io.is_negative.poke(if(num < 0) true else false)
        dut.io.dout.expect(BigInt(out) & 0xffffffffL)
        println(s"hardware  | out: ${dut.io.dout.peekInt().toInt}")
        println("pass.")
        println("======================================================")
        dut.clock.step()
        if(n > 1) {
          run(n - 1, if(nums.isEmpty) Nil else nums.tail)
        }
      }

      dut.clock.step(3)
      // run(100)
      run(2, List(1, -1))
    }
  }
}