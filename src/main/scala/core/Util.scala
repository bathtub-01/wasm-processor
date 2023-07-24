package core

import chisel3._
import chisel3.util._

abstract class OpCodeEnum extends ChiselEnum {
  def length: Int
  def litUInt(index: this.Type): UInt = {
    val u = this.all.zip((0 until length).map(_.U(getWidth.W))).find(_._1 == index).map(_._2)
    u match {
      case Some(v) => v
      case _ => throw new RuntimeException("! ILLFORMED ENUM !")
    }
  }
}