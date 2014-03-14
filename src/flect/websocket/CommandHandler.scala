package flect.websocket

trait CommandHandler {
  def apply(command: Command) = handle(command)
  def handle(command: Command): CommandResponse
}

object CommandHandler {
  def apply(func: (Command) => CommandResponse) = new CommandHandler {
    def handle(command: Command): CommandResponse = func(command)
  }
}

