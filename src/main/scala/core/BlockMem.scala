package core

import chisel3._
import chisel3.util._

/*  Implement basic memory as an independant module. This helps 
    Vivado to infer memory as BRAM instead of LUT RAM.
*/

class BlockMem(width: Int, depth: Int) extends Module {
  val io = IO(new Bundle {
    val rdAddr = Input(UInt(log2Ceil(depth).W))
    val rdData = Output(UInt(width.W))
    val wrEna = Input(Bool())
    val wrData = Input(UInt(width.W))
    val wrAddr = Input(UInt(log2Ceil(depth).W))
  })

  val mem = SyncReadMem(depth, Bits(width.W))
  io.rdData := mem.read(io.rdAddr)

  when(io.wrEna) {
    mem.write(io.wrAddr, io.wrData)
  }
}
