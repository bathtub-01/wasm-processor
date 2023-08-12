package core

import chisel3._
import chisel3.util._

object DatapathState extends ChiselEnum {
  val idle = Value
  val decoding = Value
  val executing = Value
}

/* The execution stage datapath.*/
class Datapath(width: Int) extends Module {
  require(width == 32)
  val io = IO(new Bundle {
    val instr = Input(Bits(8.W))
    val new_instr = Input(Bool())
    val leb128_din = Input(Bits(32.W))
    val stack_top = Output(Bits(32.W))
    val step = Output(Bool())
  })
  val decoder = Module(new Decoder)
  val alu = Module(new ALU(width))
  val opStack = Module(new OperationStack(width, 256))

  import DatapathState._
  val csReg = RegInit(0.U.asTypeOf(new ControlSignals))
  val dataReg = RegInit(0.U(32.W))
  val datapathSTM = RegInit(idle)
  val stepCounter = RegInit(0.U(2.W))
  
  decoder.io.instr := io.instr
  alu.io.fn := csReg.alu_fn
  alu.io.in1 := opStack.io.second_element
  alu.io.in2 := opStack.io.top_element
  alu.io.in3 := opStack.io.third_element
  opStack.io.opcode := StackOpCode.idle
  opStack.io.din := DontCare
  io.stack_top := opStack.io.top_element
  io.step := false.B

  def backIdle = {
    io.step := true.B
    stepCounter := 0.U
    when(io.new_instr) {
      dataReg := io.leb128_din
      csReg := decoder.io.control_signals
      datapathSTM := executing
    }.otherwise {
      datapathSTM := idle
    }
  }

  switch(datapathSTM) {
    is(idle) {
      when(io.new_instr) {
        dataReg := io.leb128_din
        csReg := decoder.io.control_signals
        datapathSTM := executing
      }
    }
    // is(decoding) {
    //   csReg := decoder.io.control_signals
    //   datapathSTM := executing
    // }
    is(executing) {
      when(csReg.is_alu) {
        opStack.io.din := alu.io.out
        when(stepCounter === 0.U) {
          opStack.io.opcode := csReg.stack_op
          when(csReg.stack_op === StackOpCode.pop3push1_a) {
            stepCounter := stepCounter + 1.U
          }.otherwise {
            backIdle
          }
        }.otherwise {
          opStack.io.opcode := StackOpCode.pop3push1_b
          backIdle
        }
      }.elsewhen(csReg.stack_op === StackOpCode.push1 || 
                 csReg.stack_op === StackOpCode.pop1) { // i32.const/drop
        opStack.io.opcode := csReg.stack_op
        opStack.io.din := dataReg
        backIdle
      }
    }
  }
}