import chisel3._

import core._

object Main extends App {
  emitVerilog(new OperationStack(32, 10), Array("--target-dir", "generated"))
}
