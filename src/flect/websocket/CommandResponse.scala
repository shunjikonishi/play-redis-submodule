package flect.websocket

import play.api.libs.json._

sealed abstract class CommandResponseType(val name: String) {
  def toJson = JsString(name)
}

object CommandResponseType {
  case object Json extends CommandResponseType("json")
  case object Html extends CommandResponseType("html")
  case object Text extends CommandResponseType("text")
  case object Error extends CommandResponseType("error")
  case object None extends CommandResponseType("none")

  val values = Array(Json, Html, Text, Error, None)

  def fromString(name: String) =  values.filter(_.name == name).head
}

case class CommandResponse(id: Option[Long], name: String, contentType: CommandResponseType, data: JsValue) {
  def this(name: String, data: JsValue) = this(None, name, CommandResponseType.Json, data)
  def this(name: String, data: String) = this(None, name, CommandResponseType.Text, JsString(data))

  def isNone = contentType == CommandResponseType.None
  def isError = contentType == CommandResponseType.Error

  def toJson = {
    JsObject(Seq(
      "id" -> id.map(JsNumber(_)).getOrElse(JsNull),
      "command" -> JsString(name),
      "type" -> contentType.toJson,
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
      CommandResponseType.fromString((json \ "type").as[String]),
      (json \ "data") match {
        case x: JsUndefined => JsNull
        case x: JsValue => x
      }
    )
  }

  val None = CommandResponse(scala.None, "noResponse", CommandResponseType.None, JsNull)
}

