package core

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import chisel3.experimental.BundleLiterals._

object Constant {
  def N = BitPat.N(1)
  def Y = BitPat.Y(1)
  def X = BitPat.dontCare(1)
  def ALU_XX = BitPat.dontCare(ALUOpCode.getWidth)
}

class ControlSignals extends Bundle {
  val illegal = Bool()
  val is_alu = Bool()
  val alu_fn = new ALUFunction
  val is_var_stack = Bool()
  /* val var_stack_op = ??? */
  val is_load = Bool()
  val is_store = Bool()
  val stack_op = StackOpCode() // !is_alu && !is_var_stack && pop1 = DROP; !is_alu && !is_var_stack && push1 = I32_CONSTANT
}

object InstructionDecodeTable {
  import Constant._
  import Instructions._
  def ALU(opcode: ALUOpCode.Type): BitPat = BitPat(ALUOpCode.litUInt(opcode))
  def OP_STACK(opcode: StackOpCode.Type): BitPat = BitPat(StackOpCode.litUInt(opcode))
  def default: List[BitPat] = 
                    // illegal                      is_signed      var_stack_op            
                    //    |is_alu                       |is_sub          |  is_load         stack_op
                    //    |  |    alu                   |  |is_cond_inv  |     |is_store      |  
                    //    |  |     |                    |  |  |is_var_stack    |  |           |   
                    //    |  |     |                    |  |  |  |       |     |  |           |
                     List(Y, N, ALU_XX                , X, X, X, N, /* ???, */ N, N, OP_STACK(StackOpCode.idle))
  val table: List[(BitPat, List[BitPat])] = List(
    DROP          -> List(N, N, ALU_XX                , X, X, X, N, /* ???, */ N, N, OP_STACK(StackOpCode.pop1)),
    SELECT        -> List(N, Y, ALU(ALUOpCode.select) , X, X, N, N, /* ???, */ N, N, OP_STACK(StackOpCode.pop3push1_a)),
    LOCAL_GET     -> List(N, N, ALU_XX                , X, X, X, Y, /* ???, */ N, N, OP_STACK(StackOpCode.push1)),
    LOCAL_SET     -> List(N, N, ALU_XX                , X, X, X, Y, /* ???, */ N, N, OP_STACK(StackOpCode.pop1)),
    LOCAL_TEE     -> List(N, N, ALU_XX                , X, X, X, Y, /* ???, */ N, N, OP_STACK(StackOpCode.idle)),
    GLOBAL_GET    -> List(N, N, ALU_XX                , X, X, X, Y, /* ???, */ N, N, OP_STACK(StackOpCode.push1)),
    GLOBAL_SET    -> List(N, N, ALU_XX                , X, X, X, Y, /* ???, */ N, N, OP_STACK(StackOpCode.pop1)),
    I32_LOAD      -> List(N, N, ALU_XX                , X, X, X, N, /* ???, */ Y, N, OP_STACK(StackOpCode.pop1push1)), // ?
    I32_STORE     -> List(N, N, ALU_XX                , X, X, X, N, /* ???, */ N, Y, OP_STACK(StackOpCode.pop2push1)), // ?
    I32_CONSTANT  -> List(N, N, ALU_XX                , X, X, X, N, /* ???, */ N, N, OP_STACK(StackOpCode.push1)),
    I32_EQZ       -> List(N, Y, ALU(ALUOpCode.eqz)    , X, X, N, N, /* ???, */ N, N, OP_STACK(StackOpCode.pop1push1)),
    I32_EQ        -> List(N, Y, ALU(ALUOpCode.eq)     , X, X, N, N, /* ???, */ N, N, OP_STACK(StackOpCode.pop2push1)),
    I32_NE        -> List(N, Y, ALU(ALUOpCode.eq)     , X, X, Y, N, /* ???, */ N, N, OP_STACK(StackOpCode.pop2push1)),
    I32_LT_S      -> List(N, Y, ALU(ALUOpCode.lt)     , Y, Y, N, N, /* ???, */ N, N, OP_STACK(StackOpCode.pop2push1)),
    I32_LT_U      -> List(N, Y, ALU(ALUOpCode.lt)     , N, Y, N, N, /* ???, */ N, N, OP_STACK(StackOpCode.pop2push1)),
    I32_GT_S      -> List(N, Y, ALU(ALUOpCode.le)     , Y, Y, Y, N, /* ???, */ N, N, OP_STACK(StackOpCode.pop2push1)),
    I32_GT_U      -> List(N, Y, ALU(ALUOpCode.le)     , N, Y, Y, N, /* ???, */ N, N, OP_STACK(StackOpCode.pop2push1)),
    I32_LE_S      -> List(N, Y, ALU(ALUOpCode.le)     , Y, Y, N, N, /* ???, */ N, N, OP_STACK(StackOpCode.pop2push1)),
    I32_LE_U      -> List(N, Y, ALU(ALUOpCode.le)     , N, Y, N, N, /* ???, */ N, N, OP_STACK(StackOpCode.pop2push1)),
    I32_GE_S      -> List(N, Y, ALU(ALUOpCode.lt)     , Y, Y, Y, N, /* ???, */ N, N, OP_STACK(StackOpCode.pop2push1)),
    I32_GE_U      -> List(N, Y, ALU(ALUOpCode.lt)     , N, Y, Y, N, /* ???, */ N, N, OP_STACK(StackOpCode.pop2push1)),
    I32_ADD       -> List(N, Y, ALU(ALUOpCode.add_sub), X, N, N, N, /* ???, */ N, N, OP_STACK(StackOpCode.pop2push1)),
    I32_SUB       -> List(N, Y, ALU(ALUOpCode.add_sub), X, Y, N, N, /* ???, */ N, N, OP_STACK(StackOpCode.pop2push1)),
    I32_AND       -> List(N, Y, ALU(ALUOpCode.and)    , X, X, N, N, /* ???, */ N, N, OP_STACK(StackOpCode.pop2push1)),
    I32_OR        -> List(N, Y, ALU(ALUOpCode.or)     , X, X, N, N, /* ???, */ N, N, OP_STACK(StackOpCode.pop2push1)),
    I32_XOR       -> List(N, Y, ALU(ALUOpCode.xor)    , X, X, N, N, /* ???, */ N, N, OP_STACK(StackOpCode.pop2push1)),
    I32_SHL       -> List(N, Y, ALU(ALUOpCode.shl)    , N, X, N, N, /* ???, */ N, N, OP_STACK(StackOpCode.pop2push1)),
    I32_SHR_S     -> List(N, Y, ALU(ALUOpCode.shr)    , Y, X, N, N, /* ???, */ N, N, OP_STACK(StackOpCode.pop2push1)),
    I32_SHR_U     -> List(N, Y, ALU(ALUOpCode.shr)    , N, X, N, N, /* ???, */ N, N, OP_STACK(StackOpCode.pop2push1))
  )
}

/*  Decoder only contains conbinational logics. */
class Decoder extends Module {
  val io = IO(new Bundle {
    val instr = Input(Bits(8.W))
    val control_signals = Output(new ControlSignals)
  })
  import InstructionDecodeTable._
  val outputTable = table.map(_._2).transpose
  val inputs = table.map(_._1)
  val decode_out = outputTable.zip(default).map{case (o, d) => decoder(QMCMinimizer, io.instr, TruthTable(inputs.zip(o), d))}
  import  io.control_signals._
  Seq(illegal, is_alu, alu_fn.opcode, alu_fn.is_signed, alu_fn.is_sub, alu_fn.is_cond_inv,
      is_var_stack, is_load, is_store, stack_op).zip(decode_out).foreach{case (s, o) => s match {
      case alu: ALUOpCode.Type => alu := ALUOpCode.safe(o)._1
      case stack: StackOpCode.Type => stack := StackOpCode.safe(o)._1
      case _ => s := o
    }
  }
}
