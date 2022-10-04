package example

import spinal.core._
import spinal.core.sim._
import spinal.lib._

import spinal.lib.bus.misc._
import spinal.lib.bus.amba4.axi._

import scala.math._

// companion object
object StreamFragmentWriter {
}

// write a stream of fragments (i.e. generate a packet stream)
case class StreamFragmentWriter(dataWidth : Int) extends Component {
  require(dataWidth == 32)
  val io = new Bundle {
    // this is where driveFrom() drives into
    val input = slave(Stream(Fragment(Bits(dataWidth bits))))
    // and we simply pass it on the output
    val output = master(Stream(Fragment(Bits(dataWidth bits))))
  }
  io.output << io.input

  def driveFrom(busCtrl : BusSlaveFactory, baseAddress : BigInt) = new Area {
    require(busCtrl.busDataWidth == 32)

    val busCtrlWrapped = new BusSlaveFactoryAddressWrapper(busCtrl, baseAddress)

    // drive stream from 0x0
    val outputStreamLogic = new Area {
      val streamUnbuffered = busCtrlWrapped.createAndDriveFlow(Fragment(Bits(dataWidth bits)), address = 0x0).toStream
      val (streamBuffered, fifoAvailability) = streamUnbuffered.queueWithAvailability(4)
      io.input << streamBuffered
    }
  }
}

// combines the writer with a AXI slave controller
object StreamFragmentWriterDut {
}

case class StreamFragmentWriterDut(dataWidth : Int) extends Component {
  val io = new Bundle {
    val output = master(Stream(Fragment(Bits(dataWidth bits))))
    val slave0 = slave(Axi4(Axi4Config(32, 32, 2, useQos = false, useRegion = false)))
  }

  val ctrl = new Axi4SlaveFactory(io.slave0)
  val writer = StreamFragmentWriter(dataWidth)
  val bridge = writer.driveFrom(ctrl, 0)
  io.output << writer.io.output
}

// writes a few words to the stream writer, expecting to see a stream appear
object StreamFragmentWriterDutSim {

  def main(args: Array[String]) {
    val dataWidth = 32
    val maxDataValue = scala.math.pow(2, dataWidth).intValue - 1
    val keepWidth = dataWidth/8

    printf("keepWidth=%d\n", keepWidth)

    var compiled = SimConfig
      .withFstWave
      .compile(new StreamFragmentWriterDut(dataWidth))

    compiled.doSim { dut =>

      dut.io.slave0.aw.valid #= false
      dut.io.slave0.w.valid #= false
      dut.io.output.ready #= true

      //Fork a process to generate the reset and the clock on the dut
      dut.clockDomain.forkStimulus(period = 10)

      dut.clockDomain.waitSampling()
      dut.clockDomain.waitRisingEdge()
      dut.clockDomain.waitRisingEdge()
      dut.clockDomain.waitRisingEdge()

      dut.io.slave0.aw.payload.id.assignBigInt(0)
      dut.io.slave0.aw.payload.lock.assignBigInt(0) // normal
      dut.io.slave0.aw.payload.prot.assignBigInt(2) // normal non-secure data access
      dut.io.slave0.aw.payload.burst.assignBigInt(1) // fixed address burst
      dut.io.slave0.aw.payload.len.assignBigInt(0) // 1 beat per burst
      dut.io.slave0.aw.payload.size.assignBigInt(2) // 4 bytes per beat

      dut.io.slave0.w.payload.strb.assignBigInt(0xF) // 4 bytes active per beat
      
      dut.io.slave0.aw.valid #= true
      dut.io.slave0.aw.payload.addr.assignBigInt(0x100) // driveFrom() stream 
  
      dut.io.slave0.w.valid #= true
      dut.io.slave0.w.payload.data.assignBigInt(43)
  
      dut.clockDomain.waitSamplingWhere(dut.io.slave0.aw.ready.toBoolean && dut.io.slave0.w.ready.toBoolean)
  
      dut.io.slave0.aw.valid #= false
      dut.io.slave0.w.valid #= false
  
      //Wait a rising edge on the clock
      dut.clockDomain.waitRisingEdge()



      dut.io.slave0.aw.valid #= true
      dut.io.slave0.w.valid #= true
      dut.io.slave0.w.payload.data.assignBigInt(56)
  
      dut.clockDomain.waitSamplingWhere(dut.io.slave0.aw.ready.toBoolean && dut.io.slave0.w.ready.toBoolean)
  
      dut.io.slave0.aw.valid #= false
      dut.io.slave0.w.valid #= false

      dut.clockDomain.waitRisingEdge()

      dut.io.slave0.aw.valid #= true
      dut.io.slave0.w.valid #= true
      dut.io.slave0.w.payload.data.assignBigInt(56)
  
      dut.clockDomain.waitSamplingWhere(dut.io.slave0.aw.ready.toBoolean && dut.io.slave0.w.ready.toBoolean)
  
      dut.io.slave0.aw.valid #= false
      dut.io.slave0.w.valid #= false

      dut.clockDomain.waitRisingEdge()




      dut.io.slave0.aw.valid #= true
      dut.io.slave0.w.valid #= true
      dut.io.slave0.w.payload.data.assignBigInt(56)
  
      dut.clockDomain.waitSamplingWhere(dut.io.slave0.aw.ready.toBoolean && dut.io.slave0.w.ready.toBoolean)
  
      dut.io.slave0.aw.valid #= false
      dut.io.slave0.w.valid #= false

      dut.clockDomain.waitRisingEdge()


      dut.clockDomain.waitRisingEdge()
      dut.clockDomain.waitRisingEdge()


    }
  }
}
