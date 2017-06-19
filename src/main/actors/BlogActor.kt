package actors

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.UntypedActor

class BlogActor : UntypedActor() {

    val http: ActorRef = context.system().actorOf(HttpActor.props(), "http")
    val storage: ActorRef = context.system().actorOf(StorageActor.props(), "storage")

    override fun onReceive(message: Any?) {

    }

    companion object {
        fun create(system: ActorSystem): ActorRef = system.actorOf(Props.create(BlogActor::class.java))
    }
}