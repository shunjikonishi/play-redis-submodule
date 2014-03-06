package flect.redis

import akka.actor.ActorRef
import akka.actor.Actor
import akka.actor.Props
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.Logger
import play.api.libs.iteratee.Iteratee
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Done
import play.api.libs.iteratee.Input
import play.api.libs.json.JsValue
import play.api.libs.json.JsObject
import play.api.libs.json.JsString

abstract class RoomManager[T <: Room](redis: RedisService) {
	
  private var rooms = Map.empty[String, T]
      
  def join(room: String): Future[(Iteratee[String,_], Enumerator[String])] = {
    (actor ? Join(room)).asInstanceOf[Future[(Iteratee[String,_], Enumerator[String])]]
  }
  
  protected def createRoom(name: String): T
  protected def createRoomHandler(name: String): RoomHandler = RoomHandler()
  
  protected def terminate() = {
    rooms.values.filter(_.isActive).foreach(_.close)
    rooms = Map.empty[String, T]
  }
  
  def getRoom(name: String): T = {
    val room = rooms.get(name).filter(_.isActive)
    room match {
      case Some(x) => x
      case None =>
        val ret = createRoom(name)
          rooms = rooms + (name -> ret)
          ret
    }
  }

  def error(msg: String): (Iteratee[String,_], Enumerator[String]) = {
    Logger.info("Can not connect room: " + msg)
    val in = Done[String,Unit]((),Input.EOF)
    val out =  Enumerator[String](JsObject(Seq("error" -> JsString(msg))).toString).andThen(Enumerator.enumInput(Input.EOF))
    (in, out)
  }
  
  implicit val timeout = Timeout(5 seconds)
  
  private val actor = Akka.system.actorOf(Props(new MyActor()))
  
  private sealed class Msg
  private case class Join(room: String)
  
  private class MyActor extends Actor {
    def receive = {
      case Join(room) => 
        val ret = try {
          getRoom(room).join(createRoomHandler(room))
        } catch {
          case e: Exception =>
            e.printStackTrace
            error(e.getMessage)
        }
        sender ! ret
    }
    
    override def postStop() = {
      terminate()
      super.postStop()
    }
  }
  
}