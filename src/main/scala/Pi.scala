import akka.actor.{Actor, PoisonPill}
import Actor._
import akka.routing.{Routing, CyclicIterator}
import Routing._
import akka.dispatch.Dispatchers

import java.util.concurrent.CountDownLatch

sealed trait PiMessage

case object Calculate extends PiMessage

case class Work(start: Int, nrOfElements: Int) extends PiMessage

case class Result(value: Double) extends PiMessage

object Pi extends App {

  calculate(nrOfWorkers = 4, nrOfElements = 10000, nrOfMessages = 10000)

  def calculate(nrOfWorkers: Int, nrOfElements: Int, nrOfMessages: Int) {

    // this latch is only plumbing to know when the calculation is completed
    val latch = new CountDownLatch(1)

    // create the master
    val master = actorOf(
      new Master(nrOfWorkers, nrOfMessages, nrOfElements, latch)).start()

    // start the calculation
    master ! Calculate

    // wait for master to shut down
    latch.await()
  }
}