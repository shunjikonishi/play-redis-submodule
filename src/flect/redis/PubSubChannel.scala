package flect.redis

import play.api.Logger
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Iteratee
import play.api.libs.iteratee.Concurrent
import play.api.Play.current

import akka.actor.PoisonPill
import akka.actor.Props

import com.redis.{ PubSubMessage, S, U, M, E}

/*
import com.redis.RedisClient
import com.redis.RedisClientPool
import java.net.URI
import java.io.OutputStream
import play.api.Play
import play.api.Play.current
import play.api.Logger
import play.api.libs.concurrent.Akka
import akka.actor.Props
import akka.actor.Actor
import play.api.libs.concurrent.Execution.Implicits.defaultContext
*/

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
  Logger.info("open redis channel : " + channel)
  
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
  lazy val outChannel = msgChannel
  
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
      Logger.debug("receive message from redis: " + channel + ", " + msg)
      val str = receive.map(_(msg)).getOrElse(msg)
      msgChannel.push(str)
  }
  
  def send(msg: String): Unit = {
    send(channel, msg)
  }
  
  def send(channel: String, msg: String): Unit = {
    Logger.debug("send message to redis: " + channel + ", " + msg)
    pub ! Publish(channel, msg)
  }
  
  def close = {
    Logger.info("close redis channel: " + channel)
    sub.unsubscribe
    pub ! PoisonPill
  }
  
}
