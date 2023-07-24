package core

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

import scala.util.Random
import chisel3.util.BitPat

class DecoderSpec extends AnyFreeSpec with ChiselScalatestTester {

  "Decoder should decode" in {
    test(new Decoder)/* .withAnnotations(Seq(WriteVcdAnnotation)) */ { dut =>
      dut.clock.step(3)
      import InstructionDecodeTable._
      val all_possible_inputs = table.map(_._1)
      table.foreach{case (input, res) =>
        dut.io.instr.poke(input.value)
        print(s"Inst: ${input.value.toString(16)} ")
        import dut.io.control_signals._
        Seq(illegal, is_alu, alu_fn.opcode, alu_fn.is_signed, alu_fn.is_sub, alu_fn.is_cond_inv,
           is_var_stack, is_load, is_store, stack_op).zip(res).foreach{case (io, r) => {
            io match {
              // ignore don't care bit patterns
              case alu: ALUOpCode.Type => if(r.mask != 0) alu.expect(ALUOpCode.safe(r.value.U)._1)
              case stack: StackOpCode.Type => if(r.mask != 0) stack.expect(StackOpCode.safe(r.value.U)._1)
              case b: Bool => if(r.mask != 0) b.expect(r.value)
            }
           print(s"${io.peek()} ")
          }
        }
        println()
        dut.clock.step()
      }
    }
  }
}