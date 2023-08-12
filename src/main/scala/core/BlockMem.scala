package core

import chisel3._
import chisel3.util._

/*  Implement basic memory as an independant module. This helps 
    Vivado to infer memory as BRAM instead of LUT RAM.
*/

class MemIOBundle(width: Int, depth: Int) extends Bundle {
  val rdAddr = Input(UInt(log2Ceil(depth).W))
  val rdData = Output(UInt(width.W))
  val wrEna = Input(Bool())
  val wrData = Input(UInt(width.W))
  val wrAddr = Input(UInt(log2Ceil(depth).W))
}

class BlockMem(width: Int, depth: Int) extends Module {
  val io = IO(new MemIOBundle(width, depth))

  val mem = SyncReadMem(depth, Bits(width.W))
  io.rdData := mem.read(io.rdAddr)

  when(io.wrEna) {
    mem.write(io.wrAddr, io.wrData)
  }
}

/*  We use ROM (Vec) for testing, this module has the same read/write
    timing as BlockMem.
*/
class BlockMemROM(width: Int, depth: Int) extends Module {
  val io = IO(new MemIOBundle(width, depth))

  import WASMBins._
  val mem = VecInit(alu_logic_1)
  io.rdData := RegNext(mem(io.rdAddr))

  when(io.wrEna) {
    mem(io.wrAddr) := io.wrData
  }
}