package core

import chisel3._
import chisel3.util._

/* Example wasm binaries as ROMs for testing. */

object WASMBins {
  type Bin = Seq[UInt]
  val alu_logic_1: Bin = /* VecInit */Seq(
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
  val loop_1: Bin = Seq(
    0x41.U(8.W),            // i32.const
    0x7b.U(8.W),            // -5   (i32 literal)
    0x03.U(8.W),            // loop
    0x40.U(8.W),            // void
    0x41.U(8.W),            // i32.const
    0x03.U(8.W),            // 3    (i32 literal)
    0x1a.U(8.W),            // drop
    0x03.U(8.W),            // loop
    0x40.U(8.W),            // void
    0x41.U(8.W),            // i32.const
    0x09.U(8.W),            // 9    (i32 literal)
    0x1a.U(8.W),            // drop
    0x0c.U(8.W),            // br
    0x01.U(8.W),            // 1    (br depth)
    0x41.U(8.W),            // i32.const
    0x0a.U(8.W),            // 10   (i32 literal)
    0x00.U(8.W)             // machine stop
  )
  val loop_2: Bin = Seq(
    0x41.U(8.W),            // i32.const
    0x7b.U(8.W),            // -5   (i32 literal)
    0x03.U(8.W),            // loop
    0x40.U(8.W),            // void
    0x41.U(8.W),            // i32.const
    0x03.U(8.W),            // 3    (i32 literal)
    0x41.U(8.W),            // i32.const
    0x04.U(8.W),            // 4    (i32 literal)
    0x6b.U(8.W),            // i32.sub
    0x45.U(8.W),            // i32.eqz
    0x0d.U(8.W),            // br_if
    0x00.U(8.W),            // 0    (br depth)
    0x41.U(8.W),            // i32.const
    0x02.U(8.W),            // 2    (i32 literal)
    0x41.U(8.W),            // i32.const
    0x02.U(8.W),            // 2    (i32 literal)
    0x46.U(8.W),            // i32.eq
    0x0d.U(8.W),            // br_if
    0x00.U(8.W),            // 0    (br depth)
    0x41.U(8.W),            // i32.const
    0x0a.U(8.W),            // 10   (i32 literal)
    0x00.U(8.W)             // machine stop
  )
  val block_loop1: Bin = Seq(
    0x41.U(8.W),            // i32.const
    0x7b.U(8.W),            // -5   (i32 literal)
    0x41.U(8.W),            // i32.const
    0x01.U(8.W),            // 1   (i32 literal)
    0x02.U(8.W),            // block
    0x40.U(8.W),            // void
    0x03.U(8.W),            // loop
    0x40.U(8.W),            // void
    0x41.U(8.W),            // i32.const
    0x03.U(8.W),            // 3    (i32 literal)
    0x1a.U(8.W),            // drop
    0x0c.U(8.W),            // br
    0x01.U(8.W),            // 1    (br depth)
    0x0b.U(8.W),            // end
    0x41.U(8.W),            // i32.const
    0x02.U(8.W),            // 2    (i32 literal)
    0x02.U(8.W),            // block
    0x40.U(8.W),            // void
    0x0b.U(8.W),            // end
    0x1a.U(8.W),            // drop
    0x0b.U(8.W),            // end
    0x1a.U(8.W),            // drop
    0x41.U(8.W),            // i32.const
    0x2a.U(8.W),            // 42   (i32 literal)
    0x6a.U(8.W),            // i32.add
    0x00.U(8.W)             // machine stop
  )
}