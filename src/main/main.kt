import actors.BlogActor
import akka.actor.ActorSystem
import java.util.logging.Level
import java.util.logging.Logger

fun main(argv: Array<String>) {

    val mongoLogger = Logger.getLogger("org.mongodb.driver")
    mongoLogger.level = Level.SEVERE

    val actorSystem = ActorSystem.create("yoloblog-actor-system")
    BlogActor.create(actorSystem)

    readLine()

    actorSystem.terminate()
}
