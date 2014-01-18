# play-redis-submodule
This is a submodule for playframework 2.x.
You can easily make scalabe webscoke app with this module

## Install
Command:
    play new myapp
    cd myapp
    git submodule add git@github.com:shunjikonishi/play-redis-submodule.git app/redis

build.sbt: Add following dependency
    libraryDependencies ++= Seq(
      "net.debasishg" % "redisclient_2.10" % "2.11"
    )     
    
## Usage
    package controllers
    
    import play.api._
    import play.api.mvc._
    import flect.redis.RedisService
    
    object Application extends Controller {
    
      val myRedisService = RedisService("redis://@localhost:6379")
      
      def echo = WebSocket.using[String] { _ =>
        val channel = myRedisService.createPubSub("echo")
        (channel.in, channel.out)
      }
    }
