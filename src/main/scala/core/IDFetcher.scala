package core

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

class SmallControlSignals extends Bundle {
  val should_pass = Bool() // should pass it to instruction decoder
  val have_data = Bool() // have following data
  val fast = Bool() // fast instruction
  // val cf = ControlFlowIns()
  val control_flow = Bool() // is a control flow instruction
}

/*  A small decoder is used in IDFetcher for controls in fetching 
    stage.
*/
object SmallDecodeTable {
  import Constant._
  import Instructions._
  def CF(opcode: ControlFlowIns.Type): BitPat = BitPat(ControlFlowIns.litUInt(opcode))
  def default: List[BitPat] = 
    List(Y, N, Y, N/* CF(ControlFlowIns.not) */)
  def table: List[(BitPat, List[BitPat])] = List(
    UNREACHABLE     -> List(N, N, Y, N/* CF(ControlFlowIns.not) */),
    NOP             -> List(N, N, Y, N/* CF(ControlFlowIns.not) */),
    BLOCK           -> List(N, Y, Y, Y/* CF(ControlFlowIns.block) */),
    LOOP            -> List(N, Y, Y, Y/* CF(ControlFlowIns.loop)*/),
    BR              -> List(N, Y, N, Y/* CF(ControlFlowIns.br) */),
    BR_IF           -> List(N, Y, N, Y/* CF(ControlFlowIns.br_i)*/),
    I32_CONSTANT    -> List(Y, Y, Y, N/* CF(ControlFlowIns.not) */),
    SELECT          -> List(Y, N, N, N/* CF(ControlFlowIns.not) */)
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
  Seq(should_pass,have_data, fast, control_flow).zip(decode_out).foreach{case (s, o) => s match {
      // case alu: ALUOpCode.Type => alu := ALUOpCode.safe(o)._1
      // case stack: StackOpCode.Type => stack := StackOpCode.safe(o)._1
      // case c_flow: ControlFlowIns.Type => c_flow := ControlFlowIns.safe(o)._1
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
  val walking = Value
  val stop = Value
}

/*  Instruction and data fetcher.*/
class IDFetcher extends Module {
  val io = IO(new Bundle {
    val new_pc = Flipped(ValidIO(UInt(32.W)))
    val step = Input(Bool()) // execute the next instruction
    val instr_alu = Output(Bits(8.W))
    val new_instr_alu = Output(Bool())
    val instr_cf = Output(Bits(8.W))
    val leb128_dout = Output(Bits(32.W))
    val loop_addr = Output(Bits(32.W))
  })
  import IDFetcherState._
  val pc = RegInit(0.U(32.W))
  val leb128SignedDecoder = Module(new LEB128SignedDecoder)
  val leb128UnsignedDecoder = Module(new LEB128UnsignedDecoder)
  // val wasmMem = Module(new BlockMem(8, 1024 * 16)) 
  val wasmMem = Module(new BlockMemROM(8, 1024)(WASMBins.loop_2))
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
  io.instr_alu := 0.U
  io.new_instr_alu := false.B
  io.instr_cf := 0.U
  io.leb128_dout := false.B
  io.loop_addr := 0.U

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
        STM := decoding
      }
      when(io.new_pc.valid) {
        pc := io.new_pc.bits
        STM := instr_fetch
      }
    }
    is(instr_fetch) { // 001
      STM := decoding
      pc := pc + 1.U
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
    is(processing) { // 011
      when(!csReg.have_data) {
        when(csReg.should_pass) {
          io.instr_alu := instr
          io.new_instr_alu := true.B
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
        backIdle
        when(csReg.control_flow) {
          io.instr_cf := instr
          io.leb128_dout := leb128UnsignedDecoder.io.dout
          io.loop_addr := pc - 1.U
        }.otherwise {
          io.instr_alu := instr
          io.new_instr_alu := true.B
          io.leb128_dout := leb128SignedDecoder.io.dout
        }
        leb128Data.foreach(_ := 0.U)
        leb128DataIdx := 1.U
      }.otherwise {
        leb128Data(leb128DataIdx) := readOutWire
        noMoreDataByte := !readOutWire(7)
        dataIsNeg := readOutWire(6)
        leb128DataIdx := leb128DataIdx + 1.U
        pc := pc + 1.U
      }
    }
    is(stop) {/* stop here! */} // 110
  }
}
