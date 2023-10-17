package core

import chisel3._
import chisel3.util._

object CFStackOpCode extends ChiselEnum {
  val idle = Value
  val push = Value
  val pop = Value 
}

/*  This module maintains the WASM control flow structure during runtime. 
    The stack contains:
      1. address of the first instruction for a `loop` structure
      2. all zero for a `block` structure
*/
class ControlFlowStack(width: Int, depth: Int) extends Module {
  val io = IO(new Bundle {
    val op = Input(CFStackOpCode())
    val pushed_value = Input(UInt(width.W))
    val branch_depth = Input(UInt(log2Ceil(depth).W))
    val target = Output(UInt(width.W))
  })

  val stack = RegInit(VecInit.fill(depth)(0.U(width.W)))
  val topPt = RegInit(0.U(log2Ceil(depth).W))

  import CFStackOpCode._
  switch(io.op) {
    is(idle) {}
    is(push) {
      stack(topPt) := io.pushed_value
      topPt := topPt + 1.U
    }
    is(pop) {
      when(io.target === 0.U) { // this is a block target
        topPt := topPt - io.branch_depth - 1.U
      }.otherwise {
        topPt := topPt - io.branch_depth
      }
    }
  }

  io.target := stack(topPt - 1.U - io.branch_depth)
}