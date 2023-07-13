package core

import chisel3._
import chisel3.util._

/* The implementation assumes we don't get any ill-formed input. */

class LEB128UnsignedDecoder extends Module {
  val io = IO(new Bundle {
    val din = Input(Vec(5, Bits(8.W)))
    val dout = Output(UInt(32.W))
  })
  val byte0 = Cat(io.din(1)(0   ), io.din(0)(6, 0))
  val byte1 = Cat(io.din(2)(1, 0), io.din(1)(6, 1))
  val byte2 = Cat(io.din(3)(2, 0), io.din(2)(6, 2))
  val byte3 = Cat(io.din(4)(3, 0), io.din(3)(6, 3))
  io.dout := Cat(byte3, byte2, byte1, byte0)
}

class LEB128SignedDecoder extends Module {
  val io = IO(new Bundle {
    val din = Input(Vec(5, Bits(8.W)))
    val is_negative = Input(Bool()) // let data fetcher to determine this
    val dout = Output(UInt(32.W))
  })
  val signed_din = io.din.map(byte =>  
    Mux(byte =/= 0.U, byte,
        Mux(io.is_negative, Fill(8, 1.U), 0.U))
  )
  val byte0 = Cat(signed_din(1)(0   ), signed_din(0)(6, 0))
  val byte1 = Cat(signed_din(2)(1, 0), signed_din(1)(6, 1))
  val byte2 = Cat(signed_din(3)(2, 0), signed_din(2)(6, 2))
  val byte3 = Cat(signed_din(4)(3, 0), signed_din(3)(6, 3))
  io.dout := Cat(byte3, byte2, byte1, byte0)
}
