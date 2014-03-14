package flect.websocket

trait CommandBroadcast {

	def send(res: CommandResponse): Unit
}