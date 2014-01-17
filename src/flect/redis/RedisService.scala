package flect.redis

import java.io.OutputStream
import com.redis.RedisClient
import com.redis.RedisClientPool
import com.redis.{ PubSubMessage, S, U, M, E}
import play.api.Play
import play.api.Play.current
import play.api.Logger
import play.api.libs.iteratee.Iteratee
import play.api.libs.iteratee.Concurrent
import play.api.libs.concurrent.Akka
import akka.actor.Props
import akka.actor.Actor
import akka.actor.PoisonPill

import play.api.libs.concurrent.Execution.Implicits.defaultContext

class RedisService(redisUrl: String) {
  private val (host, port, secret, pool) = {
    val uri = new java.net.URI(redisUrl)
    val host = uri.getHost
    val port = uri.getPort
    val secret = uri.getUserInfo.split(":").toList match {
      case username :: password :: Nil => Some(password)
      case _ => None
    }
    val pool = new RedisClientPool(host, port, secret=secret)
    Logger.info(s"Redis host: $host, Redis port: $port")
    (host, port, secret, pool)
  }
  
  def createClient = {
    val client = new RedisClient(host, port)
    secret.foreach(client.auth(_))
    client
  }
  
  def withClient[T](body: RedisClient => T) = pool.withClient(body)
  
  def close = pool.close
  
  def createPubSub(channel: String,
    send: String => String = null,
    receive: String => String = null,
    disconnect: => String = null,
    exception: Throwable => Unit = defaultException,
    subscribe: (String, Int) => Unit = defaultSubscribe,
    unsubscribe: (String, Int) => Unit = defaultUnsubscribe
  ) = new PubSubChannel(this, channel, 
    Option(send), Option(receive), Option(() => disconnect), 
    Option(exception), Option(subscribe), Option(unsubscribe)
  )
  
  private def defaultException: Throwable => Unit = { ex =>
    Logger.error("Subscriber error", ex)
  }
  
  private def defaultSubscribe: (String, Int) => Unit = { (c, n) =>
    Logger.info("subscribed to " + c + " and count = " + n)
  }
  
  private def defaultUnsubscribe: (String, Int) => Unit = { (c, n) =>
    Logger.info("unsubscribed from " + c + " and count = " + n)
  }
}

object RedisService {
  def apply(uri: String) = new RedisService(uri)
}

class PubSubChannel(redis: RedisService, channel: String,
  send: Option[String => String] = None,
  receive: Option[String => String] = None,
  disconnect: Option[() => String] = None,
  exception: Option[Throwable => Unit] = None,
  subscribe: Option[(String, Int) => Unit] = None,
  unsubscribe: Option[(String, Int) => Unit] = None
  ) {
  
  private val (msgEnumerator, msgChannel) = Concurrent.broadcast[String]
  private val pub = Akka.system.actorOf(Props(new Publisher(redis)))
  private val sub = {
    val client = redis.createClient
    client.subscribe(channel)(callback)
    client
  }
  Logger.info("open: " + channel)
  
  lazy val in = Iteratee.foreach[String] { msg =>
    val str = send.map(_(msg)).getOrElse(msg)
    send(str)
  }.map { _ =>
    disconnect.foreach { f =>
      val msg = f()
      send(msg)
    }
    close
  }
  
  lazy val out = msgEnumerator
  
  private def callback(pubsub: PubSubMessage): Unit = pubsub match {
    case E(ex) => 
      exception.foreach(_(ex))
    case S(channel, no) => 
      subscribe.foreach(_(channel, no))
    case U(channel, no) => 
      unsubscribe.foreach(_(channel, no))
      if (no == 0) {
        sub.disconnect
      }
    case M(channel, msg) => 
      Logger.debug("receive: " + msg)
      val str = receive.map(_(msg)).getOrElse(msg)
      msgChannel.push(str)
  }
  
  def send(msg: String) = {
    Logger.debug("send: " + msg)
    pub ! Publish(channel, msg)
  }
  
  def send(channel: String, msg: String) = {
    Logger.debug("send: " + msg)
    pub ! Publish(channel, msg)
  }
  
  def close = {
    Logger.info("close: " + channel)
    sub.unsubscribe
    pub ! PoisonPill
  }
  
}

case class Publish(channel: String, message: String)
class Publisher(redis: RedisService) extends Actor {
  def receive = {
    case Publish(c, m) =>
      val ret = redis.withClient { _.publish(c, m)}
      sender ! ret
  }
}
