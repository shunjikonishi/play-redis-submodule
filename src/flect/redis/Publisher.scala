package flect.redis

import akka.actor.Actor

case class Publish(channel: String, message: String)
class Publisher(redis: RedisService) extends Actor {
  def receive = {
    case Publish(c, m) =>
      val ret = redis.withClient { _.publish(c, m)}
      sender ! ret
  }
}
