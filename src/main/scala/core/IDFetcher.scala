package core

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import os.proc

class SmallControlSignals extends Bundle {
  val should_pass = Bool() // should pass it to instruction decoder
  val have_data = Bool() // have following data
  val fast = Bool() // fast instruction
}

/*  A small decoder is used in IDFetcher for instructions ought to 
    be executed in this stage (mainly control flow related ones).
*/
object SmallDecodeTable {
  import Constant._
  import Instructions._
  def default: List[BitPat] = 
    List(Y, N, Y)
  def table: List[(BitPat, List[BitPat])] = List(
    UNREACHABLE     -> List(N, N, Y),
    NOP             -> List(N, N, Y),
    BLOCK           -> List(N, N, Y), // ignore the block type2
    I32_CONSTANT    -> List(Y, Y, Y),
    SELECT          -> List(Y, N, N)
  )
}

class SmallDecoder extends Module {
  val io = IO(new Bundle {
    val instr = Input(Bits(8.W))
    val control_signals = Output(new SmallControlSignals)
  })
  import SmallDecodeTable._
  val outputTable = table.map(_._2).transpose
  val inputs = table.map(_._1)
  val decode_out = outputTable.zip(default).map{case (o, d) => decoder(QMCMinimizer, io.instr, TruthTable(inputs.zip(o), d))}
  import io.control_signals._
  Seq(should_pass,have_data, fast).zip(decode_out).foreach{case (s, o) => s match {
      // case alu: ALUOpCode.Type => alu := ALUOpCode.safe(o)._1
      // case stack: StackOpCode.Type => stack := StackOpCode.safe(o)._1
      case _ => s := o
    }
  }
}

object IDFetcherState extends ChiselEnum {
  val idle = Value
  val instr_fetch = Value
  val decoding = Value
  val processing = Value
  val data_fetch = Value
  val data_fetch_done = Value
  val stop = Value
}

/*  Instruction and data fetcher. Also manages control flow.*/
class IDFetcher extends Module {
  val io = IO(new Bundle {
    val new_pc = Flipped(ValidIO(UInt(32.W)))
    val step = Input(Bool()) // branch not taken, i.e. execute next instruction
    val instr = Output(Bits(8.W))
    val new_instr = Output(Bool())
    val leb128_dout = Output(Bits(32.W))
    // val context_switch = Output(Bool())
    // val output_valid = Output(Bool())
  })
  import IDFetcherState._
  val pc = RegInit(0.U(32.W))
  val leb128SignedDecoder = Module(new LEB128SignedDecoder)
  val leb128UnsignedDecoder = Module(new LEB128UnsignedDecoder)
  // val wasmMem = Module(new BlockMem(8, 1024 * 16)) // where .wasm file stays. maybe using a Rom?
  val wasmMem = Module(new BlockMemROM(8, 1024 * 16))
  val smallDecoder = Module(new SmallDecoder)
  val readOutWire = wasmMem.io.rdData
  val instr = RegInit(0.U(8.W))
  val csReg = RegInit(0.U.asTypeOf(new SmallControlSignals))
  val STM = RegInit(idle)
  val leb128Data = RegInit(VecInit.fill(5)(0.U(8.W)))
  val leb128DataWire = Wire(Vec(5, UInt(8.W)))
  val leb128DataIdx = RegInit(1.U(3.W))
  val dataIsNeg = RegInit(false.B)
  val noMoreDataByte = RegInit(false.B)

  leb128DataWire := leb128Data
  leb128SignedDecoder.io.din := leb128DataWire
  leb128SignedDecoder.io.is_negative := dataIsNeg
  leb128UnsignedDecoder.io.din := leb128DataWire
  wasmMem.io.rdAddr := pc
  wasmMem.io.wrEna := false.B
  wasmMem.io.wrAddr := 0.U
  wasmMem.io.wrData := 0.U
  smallDecoder.io.instr := readOutWire
  io.instr := 0.U
  io.new_instr := false.B
  io.leb128_dout := false.B

  def backIdle = {
    when(csReg.fast) {
      instr := readOutWire
      csReg := smallDecoder.io.control_signals
      pc := pc + 1.U // a little bit of speculation
      when(readOutWire === 0.U) {
        STM := stop
      }.otherwise {
        STM := processing
      }
      // STM := decoding
    }.otherwise {
      STM := idle
      pc := pc - 1.U
    }
  }

  switch(STM) {
    is(idle) { // 000
      when(io.step) {
        pc := pc + 1.U
        // STM := instr_fetch
        STM := decoding
      }
      when(io.new_pc.valid) {
        pc := io.new_pc.bits
        STM := instr_fetch
      }
    }
    is(instr_fetch) { // 001
      STM := decoding
    }
    is(decoding) { // 010
      instr := readOutWire
      csReg := smallDecoder.io.control_signals
      pc := pc + 1.U // a little bit of speculation
      when(readOutWire === 0.U) {
        STM := stop
      }.otherwise {
        STM := processing
      }
    }
    is(processing) { // try to only stay in this state for one cycle // 011
      when(!csReg.have_data) {
        when(csReg.should_pass) {
          io.instr := instr
          io.new_instr := true.B
        }
        backIdle
      }.otherwise { // have data
        STM := data_fetch
        leb128Data(0) := readOutWire
        // singleDataByte := !readOutWire(7)
        noMoreDataByte := !readOutWire(7)
        dataIsNeg := readOutWire(6)
        pc := pc + 1.U
      }
    }
    is(data_fetch) { // 100
      when(noMoreDataByte) {
        io.instr := instr
        io.new_instr := true.B
        io.leb128_dout := leb128SignedDecoder.io.dout
        leb128Data.foreach(_ := 0.U)
        leb128DataIdx := 1.U
        backIdle
      }.otherwise {
        leb128Data(leb128DataIdx) := readOutWire
        noMoreDataByte := !readOutWire(7)
        dataIsNeg := readOutWire(6)
        leb128DataIdx := leb128DataIdx + 1.U
      }
      pc := pc + 1.U
    }
    is(stop) {/* stop here! */} // 110
  }
}
