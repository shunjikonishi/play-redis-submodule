package flect.websocket

import play.api.Logger
import play.api.libs.json._
import play.api.libs.iteratee.Iteratee
import play.api.libs.iteratee.Concurrent
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class CommandInvoker {

  private var handlers = Map.empty[String, (Command) => Option[CommandResponse]]

  protected def defaultHandler(command: Command): Option[CommandResponse] = {
    Some(command.error("Unknown command: " + command.name))
  }

  protected def log(msg: String, res: Option[CommandResponse], time: Long): Unit = {
    val status = res.filter(_.contentType == "error").map(_ => "NG").getOrElse("OK")
    Logger.info(s"command: ${msg}, status: ${status}, time=${time}ms")
  }

  protected def onDisconnect: Unit = {}

  val (out, channel) = Concurrent.broadcast[String]
  val in = Iteratee.foreach[String] { msg =>
    val command = Command.fromJson(msg)
    handle(command).foreach(res => channel.push(res.toString))
  }.map(_ => onDisconnect)

  def handle(command: Command): Option[CommandResponse] = {
    val start = System.currentTimeMillis
    val logMsg = command.name + command.log.map("(" + _ + ")").getOrElse("")
    val handler: (Command) => Option[CommandResponse] = handlers.get(command.name).getOrElse(defaultHandler)
    val ret = try {
      handler(command)
    } catch {
      case e: Exception =>
        Logger.error(command.toString)
        Logger.error(e.getMessage, e)
        Some(command.error(e.toString))
    }
    log(logMsg, ret, System.currentTimeMillis - start)
    ret
  }

  def addHandler(command: String)(handler: (Command) => Option[CommandResponse]): Unit = {
    handlers += (command -> handler)
  }

  def addHandler(command: String, handler: CommandHandler): Unit = {
    handlers += (command -> {c: Command => Option(handler.handle(c))})
  }
}

