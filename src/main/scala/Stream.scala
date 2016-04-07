import akka.actor.ActorSystem
import akka.stream.QueueOfferResult.{Dropped, Enqueued}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import org.reactivestreams.{Publisher, Subscriber}

import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration._

object TSTream extends App{

  implicit val system = ActorSystem("mine")
  implicit val mat = ActorMaterializer()
  implicit val ec = system.dispatcher
  val timeOut = Timeout(5 seconds)

  // delayer simulates slow consumer
  def delayer(in:(String,Promise[String])): Future[String] = {
    system.scheduler.scheduleOnce(1 second){
      println(s"Done ${in._1}")
      in._2.success(in._1 + " OK")}
    in._2.future
  }


  //experiment with different strategies - dropNew dropTail dropBuffer dropHead fail and see what happens
  val src = Source.queue[(String, Promise[String])](2, OverflowStrategy.backpressure).mapAsync(1)(delayer).to(Sink.ignore).run()


  val start = System.currentTimeMillis()
  val results = for{i <- 1 to 10 } yield {
    val el = s"E_${i}"
    val p = Promise[String]
    val e = el -> p
    val x = Await.result(src.offer(e), 10 seconds)
    x match {
        case Enqueued => {println("successful enqueue, return future"); p.future}
        case Dropped => {
          println("failing drop"); Future.failed(new RuntimeException("a"))
        }
    }

  }
  val mid = System.currentTimeMillis()
  println(s"Offering ended in ${mid - start} millis")


  val r= Await.ready(Future.sequence(results), 20 minutes)
  println(s"RRR ${r}")
  val end = System.currentTimeMillis()
  println(s"Complete in ${end - start} millis")
  src.complete()
  Await.ready(system.terminate(), 20 minutes)
}