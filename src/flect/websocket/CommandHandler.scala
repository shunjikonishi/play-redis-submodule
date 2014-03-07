package flect.websocket

trait CommandHandler {
  def handle(command: Command): CommandResponse
}

object CommandHandler {
  def apply(func: (Command) => CommandResponse) = new CommandHandler {
    def handle(command: Command): CommandResponse = func(command)
  }
}

