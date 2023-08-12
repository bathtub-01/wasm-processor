package core

import chisel3._
import chisel3.util._

/* Example wasm binaries as ROMs for testing. */

object WASMBins {
  val alu_logic_1 = /* VecInit */Seq(
    0x41.U(8.W),            // i32.const
    0x7b.U(8.W),            // -5   (i32 literal)
    0x41.U(8.W),            // i32.const
    0x8e.U(8.W),            // -
    0x01.U(8.W),            // 142  (i32 literal)
    0x41.U(8.W),            // i32.const
    0x2a.U(8.W),            // 42   (i32 literal)
    0x6b.U(8.W),            // i32.sub
    0x6a.U(8.W),            // i32.add
    0x41.U(8.W),            // i32.const
    0xd9.U(8.W),            // -
    0x00.U(8.W),            // 89   (i32 literal)
    0x41.U(8.W),            // i32.const
    0x00.U(8.W),            // 0    (i32 literal)
    0x1b.U(8.W),            // select
    0x41.U(8.W),            // i32.const
    0x01.U(8.W),            // 1    (i32 literal)
    0x75.U(8.W),            // i32.shr_u
    0x1a.U(8.W),            // drop
    0x00.U(8.W)             // machine stop
  )
}