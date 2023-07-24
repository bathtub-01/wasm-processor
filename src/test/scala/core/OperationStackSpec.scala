package core

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

import scala.util.Random

class OperationStackSimulator(stack_depth: Int) {
  object opCode extends Enumeration {
    val idle, push1, pop1, pop1push1, pop2push1, pop3push1 = Value
  }

  // operates on stack based on inputs, return the operated stack
  def step(stack: List[BigInt], op: opCode.Value, din: BigInt): List[BigInt] = {
    (op, stack) match {
      case (opCode.idle, s) => s
      case (opCode.push1, s) => din :: s
      case (opCode.pop1, top :: s) => s
      case (opCode.pop1push1, top :: s) => din :: s
      case (opCode.pop2push1, top :: second :: s) => din :: s
      case (opCode.pop3push1, top :: second :: third :: s) => din :: s
      case _ => throw new RuntimeException("! ILLEGAL OPERATION !")
    }
  }

  import opCode._
  // randomly generates a legal opCode for testing
  // push1 should have more chances to be picked
  def opCodeGen(stack: List[BigInt]): opCode.Value = {
    if(stack.length == 0) {
      if (Random.nextInt(10) < 7) push1 else idle
    } else if(stack.length == 1) {
      if (Random.nextInt(10) < 7) push1 else
        Seq(idle, pop1, pop1push1)(Random.nextInt(3))
    } else if(stack.length == 2) {
      if (Random.nextInt(10) < 7) push1 else
        Seq(idle, pop1, pop1push1, pop2push1)(Random.nextInt(4))
    } else if(stack.length >= 3 && stack.length < stack_depth) {
      if (Random.nextInt(10) < 6) push1 else
        Seq(idle, pop1, pop1push1, pop2push1, pop3push1)(Random.nextInt(5))
    } else { // stack.length == stack_depth
      Seq(idle, pop1, pop1push1, pop2push1, pop3push1)(Random.nextInt(5))
    }
  }

  // return the top3 elements of a stack
  def top3(stack: List[BigInt]): (BigInt, BigInt, BigInt) = {
    stack match {
      case (top :: second :: third :: s) => (top, second, third)
      case (top :: second :: Nil) => (top, second, 0)
      case (top :: Nil) => (top, 0, 0)
      case _ => (0, 0, 0)
    }
  }
}

class OperationStackSpec extends AnyFreeSpec with ChiselScalatestTester {

  "Stack should operate" in {
    test(new OperationStack(32, 32))/* .withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) */ { dut =>
      val simulator = new OperationStackSimulator(34)

      /*  n: the number of operations to be tested
          ops: optional, specify an operation sequence manually
          if not specified, operations will be generated randomly
      */
      def run(n: Int, stack: List[BigInt], ops: List[simulator.opCode.Value] = Nil): Unit = {
        val din = BigInt(Random.nextInt(Int.MaxValue))
        val newOp = if (ops.isEmpty) simulator.opCodeGen(stack) else ops.head
        println(s"op: $newOp, din: $din")
        val newStack = simulator.step(stack, newOp, din)
        val (top, second, third) = simulator.top3(newStack)
        newOp match {
          case simulator.opCode.idle => {
            dut.io.opcode.poke(StackOpCode.idle)
            dut.clock.step()
          }
          case simulator.opCode.push1 => {
            dut.io.opcode.poke(StackOpCode.push1)
            dut.io.din.poke(din.U)
            dut.clock.step()
          }
          case simulator.opCode.pop1 => {
            dut.io.opcode.poke(StackOpCode.pop1)
            dut.clock.step()
          }
          case simulator.opCode.pop1push1 => {
            dut.io.opcode.poke(StackOpCode.pop1push1)
            dut.io.din.poke(din.U)
            dut.clock.step()
          }
          case simulator.opCode.pop2push1 => {
            dut.io.opcode.poke(StackOpCode.pop2push1)
            dut.io.din.poke(din.U)
            dut.clock.step()
          }
          case simulator.opCode.pop3push1 => {
            dut.io.opcode.poke(StackOpCode.pop3push1_a)
            dut.io.din.poke(din.U)
            dut.clock.step()
            dut.io.opcode.poke(StackOpCode.pop3push1_b)
            dut.clock.step()
          }
        }
        dut.io.top_element.expect(top)
        dut.io.second_element.expect(second)
        dut.io.third_element.expect(third)
        println(s"simulator | top: $top, second: $second, third: $third | depth: ${newStack.length}")
        println(s"hardware  | top: ${dut.io.top_element.peekInt()}, second: ${dut.io.second_element.peekInt()}, third: ${dut.io.third_element.peekInt()}")
        println("pass.")
        println("======================================================")
        if(n > 1) {
          run(n - 1, newStack, if(ops.isEmpty) Nil else ops.tail)
        }
      }

      dut.clock.step(3)
      import simulator.opCode._
      val ops = List(push1, push1, push1, push1, push1, pop1, pop1, pop1, pop1)
      // run(ops.length, Nil, ops)
      run(200, Nil, ops)

      // dut.clock.step(3)
    }
  }
}
