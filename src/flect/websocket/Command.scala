package flect.websocket

import play.api.libs.json._

case class Command(id: Long, name: String, log: Option[String], data: JsValue) {
  def error(data: String) = CommandResponse(Some(id), name, CommandResponseType.Error, JsString(data))
  def text(data: String) = CommandResponse(Some(id), name, CommandResponseType.Text, JsString(data))
  def html(data: String) = CommandResponse(Some(id), name, CommandResponseType.Html, JsString(data))
  def json(data: JsValue) = CommandResponse(Some(id), name, CommandResponseType.Json, data)

  def toJson = {
    JsObject(Seq(
      "id" -> JsNumber(id),
      "command" -> JsString(name),
      "log" -> log.map(JsString(_)).getOrElse(JsNull),
      "data" -> data
    ).filter(t => t._2 != JsNull))
  }

  override def toString = toJson.toString
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

