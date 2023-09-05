package core

import chisel3._
import chisel3.util._

object ControlFlowIns extends OpCodeEnum {
  val not = Value // not a control flow instruction
  val loop = Value
  val block = Value
  val call = Value
  val end = Value
  val ret = Value
  val br = Value
  val br_if = Value

  val length = 8
}

object ControlFlowUnitState extends ChiselEnum {
  val idle = Value
  val branching = Value
}

/* This module handles branching and function calls. */
class ControlFlowUnit extends Module {
  val io = IO(new Bundle {
    val instr = Input(Bits(8.W))
    val loop_addr = Input(Bits(32.W))
    val br_call_target = Input(UInt(32.W)) // branch depth or function idx
    val stack_top = Input(Bits(32.W))
    val consume_top = Output(Bool())
    val new_pc = ValidIO(UInt(32.W))
    val step = Output(Bool())
  })
  import ControlFlowUnitState._
  val cfStack = Module(new ControlFlowStack(32, 32))
  val STM = RegInit(idle)
  val instrReg = RegInit(0.U(8.W))
  val targetReg = RegInit(0.U(32.W))

  cfStack.io.op := CFStackOpCode.idle
  cfStack.io.pushed_value := 0.U
  cfStack.io.branch_depth := 0.U

  io.consume_top := false.B
  io.step := false.B
  io.new_pc.valid := false.B
  io.new_pc.bits := DontCare

  def jump = {
    cfStack.io.op := CFStackOpCode.pop
    cfStack.io.branch_depth := targetReg
    io.new_pc.bits := cfStack.io.target
    io.new_pc.valid := true.B
  }

  switch(STM) {
    is(idle) {
      when(io.instr === Instructions.LOOP) {
        cfStack.io.op := CFStackOpCode.push
        cfStack.io.pushed_value := io.loop_addr
      }
      when(io.instr === Instructions.BR || io.instr === Instructions.BR_IF) {
        STM := branching
        instrReg := io.instr
        targetReg := io.br_call_target
      }
    }
    is(branching) {
      when(instrReg === Instructions.BR) {
        jump
      }.otherwise { // BR_IF
        when(io.stack_top === 0.U) {
          io.step := true.B
        }.otherwise { // branch taken
          jump
        }
        io.consume_top := true.B
      }
      STM := idle
    }
  }
}