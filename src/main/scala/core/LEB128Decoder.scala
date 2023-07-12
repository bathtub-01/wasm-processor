package core

import chisel3._
import chisel3.util._

/* The implementation assumes we don't get any ill-formed input. */

class LEB128UnsignedDecoder extends Module {
  val io = IO(new Bundle {
    val din = Input(Vec(5, Bits(8.W)))
    val dout = Output(UInt(32.W))
  })
  val byte0 = Cat(Mux(io.din(0)(7), io.din(1)(0   ), 0.U(1.W)), io.din(0)(6, 0))
  val byte1 = Cat(Mux(io.din(1)(7), io.din(2)(1, 0), 0.U(2.W)), io.din(1)(6, 1))
  val byte2 = Cat(Mux(io.din(2)(7), io.din(3)(2, 0), 0.U(3.W)), io.din(2)(6, 2))
  val byte3 = Cat(Mux(io.din(3)(7), io.din(4)(3, 0), 0.U(4.W)), io.din(3)(6, 3))
  io.dout := Cat(byte3, byte2, byte1, byte0)
}

class LEB128SignedDecoder extends Module {
  val io = IO(new Bundle {
    val din = Input(Vec(5, Bits(8.W)))
    val have_one_more_zero = Input(Bool())
    val dout = Output(UInt(32.W))
  })

}
