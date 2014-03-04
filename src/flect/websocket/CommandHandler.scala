package flect.websocket

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


class CommandHandler {
  private var handlers = Map.empty[String, (Command) => CommandResponse]

  val (out, channel) = Concurrent.broadcast[String]
  val in = Iteratee.foreach[String] { msg =>
    val json = Json.parse(msg)
    val command = Command(
      (json \ "id").as[Long],
      (json \ "command").as[String],
      (json \ "log").asOpt[String],
      (json \ "data") match {
        case x: JsUndefined => JsNull
        case x: JsValue => x
      }
    )
    val res = handle(command)
    channel.push(res.toJson)
  }

  def handle(command: Command): CommandResponse = {
    handlers.get(command.name).map(_(command)).getOrElse(command.error("Unknown command: " + command.name))
  }

  def addHandler(command: String)(handler: (Command) => CommandResponse): Unit = {
    handlers += (command -> handler)
  }
}

