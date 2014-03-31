package flect.websocket

import play.api.Logger
import play.api.libs.json._
import play.api.libs.iteratee.Iteratee
import play.api.libs.iteratee.Concurrent
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class CommandInvoker extends CommandHandler {

  private var handlers = Map.empty[String, CommandHandler]

  private val unknownCommandHandler = CommandHandler { command =>
    command.error("Unknown command: " + command.name)
  }

  protected def defaultHandler = unknownCommandHandler

  protected def log(msg: String, res: CommandResponse, time: Long): Unit = {
    val status = if (res.isError) "NG" else "OK"
    Logger.info(s"command: ${msg}, status: ${status}, time=${time}ms")
  }

  protected def onDisconnect: Unit = {}

  val (out, channel) = Concurrent.broadcast[String]
  val in = Iteratee.foreach[String] { msg =>
    try {
      val command = Command.fromJson(msg)
      val res = handle(command)
      if (!res.isNone) {
        channel.push(res.toString)
      }
    } catch {
      case e: Exception =>
        e.printStackTrace
    }
  }.map(_ => onDisconnect)

  def handle(command: Command): CommandResponse = {
    val start = System.currentTimeMillis
    val logMsg = command.name + command.log.map("(" + _ + ")").getOrElse("")
    val handler = handlers.get(command.name).getOrElse(defaultHandler)
    val ret = try {
      handler(command)
    } catch {
      case e: Exception =>
        Logger.error(command.toString)
        Logger.error(e.getMessage, e)
        command.error(e.toString)
    }
    log(logMsg, ret, System.currentTimeMillis - start)
    ret
  }

  def addHandler(command: String)(handler: (Command) => CommandResponse): Unit = {
    handlers += (command -> CommandHandler(handler))
  }

  def addHandler(command: String, handler: CommandHandler): Unit = {
    handlers += (command -> handler)
  }
}

