package core

import chisel3._
import chisel3.util._

object CFStackOpCode extends ChiselEnum {
  val idle = Value
  val push = Value
  val pop = Value // should not use this?
  // val search = Value
}

/*  This module maintains the WASM control flow structure during runtime. 
    The stack contains:
      1. address of the first instruction for a `loop` structure
      2. all "1" for a `block` structure
*/
class ControlFlowStack(width: Int, depth: Int) extends Module {
  val io = IO(new Bundle {
    val op = Input(CFStackOpCode())
    val pushed_value = Input(UInt(width.W))
    // val poped_count = Input(UInt(log2Ceil(depth).W))
    // val search_depth = Input(UInt(log2Ceil(depth).W))
    val branch_depth = Input(UInt(log2Ceil(depth).W))
    val target = Output(UInt(width.W))
  })

  val stack = RegInit(VecInit.fill(depth)(0.U(width.W)))
  val topPt = RegInit(0.U(log2Ceil(depth).W))

  // io.search_result := DontCare

  import CFStackOpCode._
  switch(io.op) {
    is(idle) {}
    is(push) {
      stack(topPt) := io.pushed_value
      topPt := topPt + 1.U
    }
    is(pop) {
      topPt := topPt - io.branch_depth
    }
    // is(search) {
    //   io.search_result := stack(topPt - 1.U - io.search_depth)
    // }
  }

  io.target := stack(topPt - 1.U - io.branch_depth)
}