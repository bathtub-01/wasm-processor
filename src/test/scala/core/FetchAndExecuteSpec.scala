package core

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class IDFetcher_Datapath extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val stack_top = Output(Bits(32.W))
  })
  val f = Module(new IDFetcher)
  val d = Module(new Datapath(32))
  val cf = Module(new ControlFlowUnit)
  f.io.new_pc <> cf.io.new_pc
  f.io.step := io.start | d.io.step | cf.io.step
  d.io.instr := f.io.instr_alu
  d.io.leb128_din := f.io.leb128_dout
  d.io.new_instr := f.io.new_instr_alu
  d.io.consume_top := cf.io.consume_top
  cf.io.instr := f.io.instr_cf
  cf.io.loop_addr := f.io.loop_addr
  cf.io.br_call_target := f.io.leb128_dout
  cf.io.stack_top := d.io.stack_top
  io.stack_top := d.io.stack_top
}

class FetchAndexecuteSpec extends AnyFreeSpec with ChiselScalatestTester {

  "Fetch and execute" in {
    test(new IDFetcher_Datapath).withAnnotations(Seq(WriteVcdAnnotation/* , VerilatorBackendAnnotation */)) { dut =>
      dut.clock.step(3)
      dut.io.start.poke(true)
      dut.clock.step()
      dut.io.start.poke(false)
      dut.clock.step(80)
    }
  }
}

class IDFetcherSpec extends AnyFreeSpec with ChiselScalatestTester {

  "IDFetcher should fetch" in {
    test(new IDFetcher).withAnnotations(Seq(WriteVcdAnnotation/* , VerilatorBackendAnnotation */)) { dut =>
      dut.clock.step(3)
      dut.io.step.poke(true)  
      dut.clock.step()
      dut.io.step.poke(false)
      dut.clock.step(16)
      dut.io.step.poke(true)  
      dut.clock.step()
      dut.io.step.poke(false)
      dut.clock.step(10)
    }
  }
}

class DatapathSpec extends AnyFreeSpec with ChiselScalatestTester {

  "Datapath should execute" in {
    test(new Datapath(32)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.step(3)
      import Instructions._
      dut.io.instr.poke(I32_CONSTANT.value)
      dut.io.leb128_din.poke(42)
      dut.io.new_instr.poke(true)
      dut.clock.step(1)
      dut.io.leb128_din.poke(20)
      dut.clock.step(1)
      dut.io.instr.poke(I32_SUB.value)
      dut.clock.step(1)
      dut.io.instr.poke(I32_CONSTANT.value)
      dut.io.leb128_din.poke(89)
      dut.clock.step(1)
      dut.io.instr.poke(I32_CONSTANT.value)
      dut.io.leb128_din.poke(0)
      dut.clock.step(1)
      dut.io.instr.poke(SELECT.value)
      dut.clock.step()
      dut.io.new_instr.poke(false)
      dut.clock.step(4)
    }
  }
}