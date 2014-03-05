package flect.websocket

import play.api.Logger
import play.api.libs.json._
import play.api.libs.iteratee.Iteratee
import play.api.libs.iteratee.Concurrent
import play.api.libs.concurrent.Execution.Implicits.defaultContext

case class Command(id: Long, name: String, log: Option[String], data: JsValue) {
  def error(data: String) = new CommandResponse(id, name, "error", JsString(data))
  def text(data: String) = new CommandResponse(id, name, "text", JsString(data))
  def html(data: String) = new CommandResponse(id, name, "html", JsString(data))
  def json(data: JsValue) = new CommandResponse(id, name, "json", data)
}

object Command {
  def fromJson(str: String) = {
    val json = Json.parse(str)
    Command(
      (json \ "id").as[Long],
      (json \ "command").as[String],
      (json \ "log").asOpt[String],
      (json \ "data") match {
        case x: JsUndefined => JsNull
        case x: JsValue => x
      }
    )
  }
}
case class CommandResponse(id: Option[Long], name: String, contentType: String, data: JsValue) {
  def this(id: Long, name: String, contentType: String, data: JsValue) = this(Some(id), name, contentType, data)
  def this(name: String, contentType: String, data: JsValue) = this(None, name, contentType, data)

  def toJson = {
    JsObject(Seq(
      "id" -> id.map(JsNumber(_)).getOrElse(JsNull),
      "command" -> JsString(name),
      "type" -> JsString(contentType),
      "data" -> data
    )).toString
  }
}

trait CommandHandler {

  def handle(command: Command): CommandResponse
}

class CommandInvoker {

  private var handlers = Map.empty[String, (Command) => Option[CommandResponse]]

  protected def defaultHandler(command: Command): Option[CommandResponse] = {
    Some(command.error("Unknown command: " + command.name))
  }

  val (out, channel) = Concurrent.broadcast[String]
  val in = Iteratee.foreach[String] { msg =>
    val command = Command.fromJson(msg)
    handle(command).foreach(res => channel.push(res.toJson))
  }

  def handle(command: Command): Option[CommandResponse] = {
    val log = "command: " + command.name + command.log.map(", params: " + _).getOrElse("")
    Logger.info(log)

    val handler: (Command) => Option[CommandResponse] = handlers.get(command.name).getOrElse(defaultHandler)
    handler(command)
  }

  def addHandler(command: String)(handler: (Command) => Option[CommandResponse]): Unit = {
    handlers += (command -> handler)
  }

  def addHandler(command: String, handler: CommandHandler): Unit = {
    handlers += (command -> {c: Command => Option(handler.handle(c))})
  }
}

