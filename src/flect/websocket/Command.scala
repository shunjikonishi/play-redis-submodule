package flect.websocket

import play.api.libs.json._

case class Command(id: Long, name: String, log: Option[String], data: JsValue) {
  def error(data: String) = CommandResponse(Some(id), name, "error", JsString(data))
  def text(data: String) = CommandResponse(Some(id), name, "text", JsString(data))
  def html(data: String) = CommandResponse(Some(id), name, "html", JsString(data))
  def json(data: JsValue) = CommandResponse(Some(id), name, "json", data)

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

case class CommandResponse(id: Option[Long], name: String, contentType: String, data: JsValue) {
  def this(name: String, contentType: String, data: JsValue) = this(None, name, contentType, data)
  def this(name: String, data: String) = this(None, name, "text", JsString(data))

  def toJson = {
    JsObject(Seq(
      "id" -> id.map(JsNumber(_)).getOrElse(JsNull),
      "command" -> JsString(name),
      "type" -> JsString(contentType),
      "data" -> data
    ).filter(t => t._2 != JsNull))
  }

  override def toString = toJson.toString
}

object CommandResponse {
  def fromJson(str: String) = {
    val json = Json.parse(str)
    CommandResponse(
      (json \ "id").asOpt[Long],
      (json \ "command").as[String],
      (json \ "type").as[String],
      (json \ "data") match {
        case x: JsUndefined => JsNull
        case x: JsValue => x
      }
    )
  }
}