package core

import chisel3._
import chisel3.util._

/*  The implementation of ALU in this file is highly inspired by the rocket-chip project:
    https://github.com/chipsalliance/rocket-chip/blob/master/src/main/scala/rocket/ALU.scala
*/

object ALUOpCode extends OpCodeEnum {
  val eqz = Value
  val eq = Value
  val le = Value
  val lt = Value
  val and = Value
  val or = Value
  val xor = Value
  val shl = Value
  val shr = Value
  // val rotl = Value
  // val rotr = Value
  val select = Value
  val add_sub = Value

  // BitPat only accept literal UInt
  def length = 11
  // def litUInt(index: this.Type): UInt = {
  //   val u = this.all.zip((0 until length).map(_.U(getWidth.W))).find(_._1 == index).map(_._2)
  //   u match {
  //     case Some(v) => v
  //     case _ => throw new RuntimeException("! ILLFORMED ENUM !")
  //   }
  // }
}

/*  is_signed: specify if an instruction is in signed version. Must be cleared for shl.
    is_sub: specify add/sub for adder. Must assert for both substraction and comparison.
    is_cond_inv: inverse the result of conditional test (eq -> neq, le -> gt, lt -> ge). 
                 Must be cleared for instructions other than comparison.
*/
class ALUFunction extends Bundle {
  val opcode = ALUOpCode()
  val is_signed = Bool() 
  val is_sub = Bool() 
  val is_cond_inv = Bool() // must clear for instructions other than comparison
}

/*  ALU only contains conbinational logics. */
class ALU(width: Int) extends Module {
  val io = IO(new Bundle {
    val in1 = Input(Bits(width.W))
    val in2 = Input(Bits(width.W))
    val in3 = Input(Bits(width.W))
    val fn = Input(new ALUFunction)
    val out = Output(Bits(width.W))
  })
  // add/sub
  val in2_inv = Mux(io.fn.is_sub, ~io.in2, io.in2)
  // val in1_xor_in2 = io.in1 ^ in2_inv
  val adder_out = io.in1 + in2_inv + io.fn.is_sub

  // conditions
  val cmp2 = Mux(io.fn.opcode === ALUOpCode.eqz, 0.U, io.in2)
  val eq = io.in1 === cmp2
  // val lt = io.in1 < cmp2
  val lt = Mux(io.in1(width - 1) === io.in2(width - 1), adder_out(width - 1),
               Mux(io.fn.is_signed, io.in1(width - 1), io.in2(width - 1)))
  val cond_out_default = Mux(io.fn.opcode <= ALUOpCode.le, eq, 0.U) |
                         Mux(io.fn.opcode === ALUOpCode.le || io.fn.opcode === ALUOpCode.lt, lt, 0.U)
  val cond_out = io.fn.is_cond_inv ^ cond_out_default
  
  // selector
  val selector_out = Mux(io.fn.opcode === ALUOpCode.select, Mux(io.in2 === 0.U, io.in1, io.in3), 0.U)

  // and/or/xor
  val logic_out = Mux(io.fn.opcode === ALUOpCode.xor || io.fn.opcode === ALUOpCode.or, io.in1 ^ io.in2, 0.U) |
                  Mux(io.fn.opcode === ALUOpCode.or || io.fn.opcode === ALUOpCode.and, io.in1 & io.in2, 0.U)
  
  // shifter
  val shamt = io.in2(4, 0)
  val sh_in = Mux(io.fn.opcode === ALUOpCode.shr, io.in1, Reverse(io.in1))
  val sh_out_r = (Cat(io.fn.is_signed & sh_in(width - 1), sh_in).asSInt >> shamt)(width - 1, 0)
  val sh_out_l = Reverse(sh_out_r)
  val sh_out = Mux(io.fn.opcode === ALUOpCode.shr, sh_out_r, 0.U) |
               Mux(io.fn.opcode === ALUOpCode.shl, sh_out_l, 0.U)
  
  val cond_selector_logic_sh_out = cond_out | selector_out | logic_out | sh_out
  io.out := Mux(io.fn.opcode === ALUOpCode.add_sub, adder_out, cond_selector_logic_sh_out)
}