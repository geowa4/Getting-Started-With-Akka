import akka.actor._
import Actor._
import java.util.concurrent.CountDownLatch
import akka.routing._
import Routing._

class Master(
  nrOfWorkers: Int, nrOfMessages: Int, nrOfElements: Int, latch: CountDownLatch)
  extends Actor {

  var pi: Double = _
  var nrOfResults: Int = _
  var start: Long = _

  // create the workers
  val workers = Vector.fill(nrOfWorkers)(actorOf[Worker].start())

  // wrap them with a load-balancing router
  val router = Routing.loadBalancerActor(CyclicIterator(workers)).start()

  def receive = {
    case Calculate =>
      // schedule work
      for (i <- 0 until nrOfMessages) router ! Work(i * nrOfElements, nrOfElements)

      // send a PoisonPill to all workers telling them to shut down themselves
      router ! Broadcast(PoisonPill)

      // send a PoisonPill to the router, telling him to shut himself down
      router ! PoisonPill

    case Result(value) =>
      // handle result from the worker
      pi += value
      nrOfResults += 1
      if (nrOfResults == nrOfMessages) self.stop()
}

  override def preStart() {
    start = System.currentTimeMillis
  }

  override def postStop() {
    // tell the world that the calculation is complete
    println(
      "\n\tPi estimate: \t\t%s\n\tCalculation time: \t%s millis"
      .format(pi, (System.currentTimeMillis - start)))
    latch.countDown()
  }
}