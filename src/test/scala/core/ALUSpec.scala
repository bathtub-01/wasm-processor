package core

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

import scala.util.Random

object ALUSimulator {
  object opCode extends Enumeration {
    val select, eqz, eq, ne, lt_s, lt_u, gt_s, gt_u, le_s, le_u, ge_s, ge_u,
        add, sub, and, or, xor, shl, shr_s, shr_u = Value
  }
  def calculate(in1: Int, in2: Int, in3: Int, op: opCode.Value): Int = op match {
  case opCode.select  => if (in2 == 0) in1 else in3
  case opCode.eqz     => if (in1 == 0) 1 else 0
  case opCode.eq      => if (in1 == in2) 1 else 0
  case opCode.ne      => if (in1 != in2) 1 else 0
  case opCode.lt_s    => if (in1 < in2) 1 else 0
  case opCode.lt_u    => if ((in1 & 0xffffffffL) < (in2 & 0xffffffffL)) 1 else 0
  case opCode.gt_s    => if (in1 > in2) 1 else 0
  case opCode.gt_u    => if ((in1 & 0xffffffffL) > (in2 & 0xffffffffL)) 1 else 0
  case opCode.le_s    => if (in1 <= in2) 1 else 0
  case opCode.le_u    => if ((in1 & 0xffffffffL) <= (in2 & 0xffffffffL)) 1 else 0
  case opCode.ge_s    => if (in1 >= in2) 1 else 0
  case opCode.ge_u    => if ((in1 & 0xffffffffL) >= (in2 & 0xffffffffL)) 1 else 0
  case opCode.add     => in1 + in2
  case opCode.sub     => in1 - in2
  case opCode.and     => in1 & in2
  case opCode.or      => in1 | in2
  case opCode.xor     => in1 ^ in2
  case opCode.shl     => in1 << (in2 % 32)
  case opCode.shr_s   => in1 >> (in2 % 32)
  case opCode.shr_u   => in1 >>> (in2 % 32)
  }
  def opCodeGen: opCode.Value = opCode(Random.nextInt(opCode.values.size))
}

class ALUSpec extends AnyFreeSpec with ChiselScalatestTester {
  import ALUSimulator._
  def poke(op: ALUOpCode.Type, is_signed: Boolean, is_sub: Boolean, is_cond_inv: Boolean)(implicit dut: ALU) = {
    dut.io.fn.opcode.poke(op)
    dut.io.fn.is_signed.poke(is_signed)
    dut.io.fn.is_sub.poke(is_sub)
    dut.io.fn.is_cond_inv.poke(is_cond_inv)
  }

  "ALU should calculate randomly" in {
    test(new ALU(32))/*.withAnnotations(Seq(WriteVcdAnnotation))*/ { implicit dut =>
      def run(n: Int, ops: List[opCode.Value] = Nil): Unit = {
        val newOp = if (ops.isEmpty) opCodeGen else ops.head
        // this can generate more reasonable input for random tests
        val in1 = if (newOp == opCode.eqz && Random.nextInt(10) < 5) 0 else Random.nextInt()
        val in2 = if ((newOp == opCode.eq || (newOp >= opCode.le_s && newOp <= opCode.ge_u)) && Random.nextInt(10) < 5) 
                    in1 else Random.nextInt()
        val in3 = Random.nextBoolean()
        val out = ALUSimulator.calculate(in1, in2, if(in3) 1 else 0, newOp)
        println(s"op: $newOp, in1: $in1, in2: $in2, in3: ${if(in3) 1 else 0}")
        println(s"simulator | out(Int): $out, out(BigInt): ${BigInt(out) & 0xffffffffL}")

        dut.io.in1.poke((BigInt(in1) & 0xffffffffL).U)
        dut.io.in2.poke((BigInt(in2) & 0xffffffffL).U)
        dut.io.in3.poke((if(in3) 1 else 0).U)
        newOp match {
          case opCode.select  => poke(ALUOpCode.select, false, false, false)
          case opCode.eqz     => poke(ALUOpCode.eqz, false, true, false)
          case opCode.eq      => poke(ALUOpCode.eq, false, true, false)
          case opCode.ne      => poke(ALUOpCode.eq, false, true, true)
          case opCode.lt_s    => poke(ALUOpCode.lt, true, true, false)
          case opCode.lt_u    => poke(ALUOpCode.lt, false, true, false)
          case opCode.gt_s    => poke(ALUOpCode.le, true, true, true)
          case opCode.gt_u    => poke(ALUOpCode.le, false, true, true)
          case opCode.le_s    => poke(ALUOpCode.le, true, true, false)
          case opCode.le_u    => poke(ALUOpCode.le, false, true, false)
          case opCode.ge_s    => poke(ALUOpCode.lt, true, true, true)
          case opCode.ge_u    => poke(ALUOpCode.lt, false, true, true)
          case opCode.add     => poke(ALUOpCode.add_sub, false, false, false)
          case opCode.sub     => poke(ALUOpCode.add_sub, false, true, false)
          case opCode.and     => poke(ALUOpCode.and, false, false, false)
          case opCode.or      => poke(ALUOpCode.or, false, false, false)
          case opCode.xor     => poke(ALUOpCode.xor, false, false, false)
          case opCode.shl     => poke(ALUOpCode.shl, false, false, false)
          case opCode.shr_s   => poke(ALUOpCode.shr, true, false, false)
          case opCode.shr_u   => poke(ALUOpCode.shr, false, false, false)
        }
        dut.io.out.expect((BigInt(out) & 0xffffffffL).U)
        println(s"hardware  | out: ${dut.io.out.peekInt()}")
        println("pass.")
        println("======================================================")
        if(n > 1) {
          run(n - 1, if(ops.isEmpty) Nil else ops.tail)
        }
      }

      dut.clock.step(3)
      run(500)
      // run(40, List.fill(20)(opCode.eqz) ++ List.fill(20)(opCode.eq))
    }
  }
}