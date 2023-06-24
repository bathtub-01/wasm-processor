package core

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

/*  Micro-op for the stack. Each operation takes a single cycle. 
*/
object StackOpCode extends ChiselEnum {
  val idle = Value
  val push1 = Value
  val pop1 = Value
  val pop1push1 = Value
  val pop2push1 = Value
  val pop3push1_a = Value
  val pop3push1_b = Value
}

object StackErrCode extends ChiselEnum {
  val noErr = Value
  val stackUnderFlow = Value
}

class ElementInReg(width: Int) extends Bundle {
  val valid = Bool() // showing if the data is valid
  val data = Bits(width.W)
}

/*  To reduce redundent read/write over the memory, it is better to use
    registers to hold the top-three elements of the stack (an operation 
    could at most consume three operands). 
*/
class OperationStack(width: Int, size: Int) extends Module {
  val io = IO(new Bundle {
    val opcode = Input(StackOpCode())
    val din = Input(Bits(width.W))
    val top_element = Output(Bits(width.W))
    val second_element = Output(Bits(width.W))
    val third_element = Output(Bits(width.W))
    val full = Output(Bool()) 
    val debug = Output(Bits(width.W))
  })
  val emptyVal = new ElementInReg(width).Lit(_.valid -> false.B, _.data -> 0.U)
  val top = RegInit(emptyVal)
  val second = RegInit(emptyVal)
  val third = RegInit(emptyVal)
  val lastPushed = RegInit(0.U(32.W)) // avoid read-after-write
  val justPushed = RegInit(false.B)
  val stk = SyncReadMem(size, Bits(width.W))
  val stkPtr = RegInit(0.U(log2Ceil(size).W))
  val stkIndex = WireInit(0.U(log2Ceil(size).W))
  val memReadEn = WireInit(true.B)
  val memDataOut = stk.read(stkIndex, memReadEn)
  io.debug := memDataOut

  def empty: Bool = stkPtr === 0.U
  def eat = {
    top.valid := true.B
    top.data := io.din
  }
  def drag = {
    second := third
    when(empty) {
      third := emptyVal
    }.otherwise {
      third.valid := true.B
      when(justPushed) {
        third.data := lastPushed
      }.otherwise {
        third.data := memDataOut
      }
      stkIndex := stkPtr - 2.U
      stkPtr := stkPtr - 1.U
    }
  }

  import StackOpCode._
  switch(io.opcode) {
    is(idle) {
      stkIndex := stkPtr - 1.U
      justPushed := false.B
    }
    is(push1) {
      eat
      second := top
      third := second
      when(third.valid) {
        memReadEn := false.B
        stk.write(stkPtr, third.data)
        stkPtr := stkPtr + 1.U
        lastPushed := third.data
        justPushed := true.B
        stkIndex := stkPtr - 1.U
      }
    }
    is(pop1) {
      top := second
      drag
      justPushed := false.B
    }
    is(pop1push1) {
      eat
      stkIndex := stkPtr - 1.U
      justPushed := false.B
    }
    is(pop2push1) {
      eat
      drag
      justPushed := false.B
    }
    is(pop3push1_a) {
      eat
      drag
      justPushed := false.B
    }
    is(pop3push1_b) {
      drag
    }
  }

  io.full := stkPtr === size.U - 1.U
  io.top_element := top.data
  io.second_element := second.data
  io.third_element := third.data
}