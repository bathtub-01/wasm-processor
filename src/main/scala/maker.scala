import chisel3._

import core._

object Main extends App {
  // emitVerilog(new OperationStack(128, 4096), Array("--target-dir", "generated"))
  // emitVerilog(new ALU(32), Array("--target-dir", "generated"))
  // emitVerilog(new LEB128SignedDecoder, Array("--target-dir", "generated"))
  // emitVerilog(new Decoder, Array("--target-dir", "generated"))
  // emitVerilog(new BlockMem(128, 4096), Array("--target-dir", "generated"))
  // emitVerilog(new Datapath(32), Array("--target-dir", "generated"))
  emitVerilog(new ControlFlowStack(32, 32), Array("--target-dir", "generated"))
}
