import chisel3._

import core._

object Main extends App {
  // emitVerilog(new OperationStack(32, 10), Array("--target-dir", "generated"))
  // emitVerilog(new ALU(32), Array("--target-dir", "generated"))
  emitVerilog(new LEB128SignedDecoder, Array("--target-dir", "generated"))
}
