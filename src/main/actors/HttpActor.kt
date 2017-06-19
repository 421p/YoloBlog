package actors

import akka.actor.Props
import akka.actor.UntypedActor
import http.Router

class HttpActor : UntypedActor() {

    val router = Router(context)

    override fun onReceive(message: Any?) {

    }

    override fun postStop() {
        router.shutdown()
    }

    companion object {
        fun props(): Props = Props.create(HttpActor::class.java)
    }
}