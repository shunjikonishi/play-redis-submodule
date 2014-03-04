package flect.redis

import akka.actor.Actor
import akka.actor.Props
import akka.actor.PoisonPill
import akka.util.Timeout
import akka.pattern.ask

import scala.concurrent.duration.Duration
import scala.concurrent.Future
import scala.concurrent.Await

import play.api.Play.current
import play.api.libs.iteratee.Iteratee
import play.api.libs.iteratee.Concurrent
import play.api.libs.iteratee.Enumerator
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent.Akka

case class RoomHandler(
  onClientMsg: Option[String=>Option[String]] = None,
  onRedisMsg: Option[String=>Option[String]] = None,
  onDisconnect: Option[()=>Any] = None
) {
  def clientMsg(f: String=>Option[String]) = copy(onClientMsg=Some(f))
  def redisMsg(f: String=>Option[String]) = copy(onRedisMsg=Some(f))
  def disconnect(f: ()=>Any) = copy(onDisconnect=Some(f))
}

class Room(name: String, redis: RedisService) {

  implicit val timeout = Timeout(5)
  private var connectCount = 0
  private var active = true
  private val actor = Akka.system.actorOf(Props(new MyActor()))
  
  def isActive = active
  def isClosed = !active
  def memberCount = connectCount
  
  val channel = redis.createPubSub(name)
  
  private sealed class RoomMessage
  private case class RoomConnect(handler: RoomHandler) extends RoomMessage
  private case object RoomDisconnect extends RoomMessage
  private case object RoomClose extends RoomMessage
  
  private class MyActor extends Actor {
    def receive = {
      case RoomConnect(h) =>
        connectCount += 1
        val in = Iteratee.foreach[String] { clientMsg =>
          val redisMsg = h.onClientMsg.map(_(clientMsg)).getOrElse(Some(clientMsg))
          redisMsg.foreach(channel.send(_))
        }.map { _ =>
          h.onDisconnect.foreach(_())
          self ! RoomDisconnect
        }
        val out = h.onRedisMsg.map { onRedisMsg =>
          val (e, c) = Concurrent.broadcast[String]
          val i = Iteratee.foreach[String] { redisMsg =>
            val clientMsg = onRedisMsg(redisMsg)
            clientMsg.foreach(c.push(_))
          }
          channel.out(i)
          e
        }.getOrElse(channel.out)
        sender ! (in, out)
      case RoomDisconnect =>
        if (isActive && connectCount > 0) {
          connectCount -= 1
          if (connectCount == 0) {
            self ! RoomClose
          }
        }
        sender ! connectCount
      case RoomClose =>
        active = false
        connectCount = 0
        channel.close
    }
  }
  
  def join(handler: RoomHandler): (Iteratee[String,_], Enumerator[String]) = {
    if (isClosed) {
      throw new IllegalStateException("Room %s already closed.".format(name))
    }
    val ret = (actor ? RoomConnect(handler)).asInstanceOf[Future[(Iteratee[String,_], Enumerator[String])]]
    Await.result(ret, Duration.Inf)
  }
  def quit: Int = {
    val ret = (actor ? RoomDisconnect).asInstanceOf[Future[Int]]
    Await.result(ret, Duration.Inf)
  }
  
  def close = {
    actor ! RoomClose
    actor ! PoisonPill
  }
}

