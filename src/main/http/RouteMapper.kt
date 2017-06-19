package http

import akka.NotUsed
import akka.actor.ActorContext
import akka.actor.ActorSelection
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.javadsl.model.HttpRequest
import akka.http.javadsl.model.HttpResponse
import akka.http.javadsl.model.MediaTypes
import akka.http.javadsl.server.AllDirectives
import akka.http.javadsl.server.PathMatchers.uuidSegment
import akka.http.javadsl.server.Route
import akka.http.javadsl.server.directives.LogEntry
import akka.http.javadsl.unmarshalling.Unmarshaller
import akka.pattern.Patterns
import akka.stream.ActorMaterializer
import akka.stream.javadsl.Flow
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import entities.Post
import messages.*
import scala.compat.java8.FutureConverters
import java.util.concurrent.CompletionStage

class RouteMapper(val router: Router) : AllDirectives() {

    val context: ActorContext = router.context
    val storage: ActorSelection = context.actorSelection("../storage")
    val mapper: ObjectMapper = router.mapper

    fun flow(system: ActorSystem, materializer: ActorMaterializer): Flow<HttpRequest, HttpResponse, NotUsed>
            = buildRoute().flow(system, materializer)

    private fun buildUserRoute(): Route = pathPrefix("post", {
        route(
                pathEndOrSingleSlash {
                    route(
                            get {
                                onSuccess(
                                        { ask(storage, GetAllPosts()) },
                                        { this.json(it) }
                                )
                            }
                    )
                },

                path(uuidSegment(), { id ->
                    route(
                            get {
                                onSuccess(
                                        { ask(storage, GetPost(id)) },
                                        { this.json(it) }
                                )
                            }
                    )
                }))
    }
    )

    private fun buildAdminRoute(): Route = pathPrefix("admin-post", {
        route(
                pathEndOrSingleSlash {
                    route(
                            get {
                                onSuccess(
                                        { ask(storage, GetAllPosts()) },
                                        { this.json(it) }
                                )
                            },

                            post {
                                entity(Unmarshaller.entityToString(), { str ->
                                    val post = mapper.readValue(str, Post::class.java)

                                    onSuccess(
                                            { ask(storage, AddPost(post)) },
                                            { this.json(it) }
                                    )
                                })
                            },

                            put {
                                entity(Unmarshaller.entityToString(), { str ->
                                    val post = mapper.readValue(str, Post::class.java)

                                    onSuccess(
                                            { ask(storage, UpdatePost(post)) },
                                            { this.json(it) }
                                    )
                                })
                            }
                    )
                },

                path(uuidSegment(), { id ->
                    route(
                            delete {
                                onSuccess(
                                        { ask(storage, DeletePost(id)) },
                                        { this.json(it) }
                                )
                            }
                    )
                }))
    })

    private fun buildRoute(): Route = route(
            logRequest({
                LogEntry.create(it.uri, Logging.InfoLevel())
            }, {
                route(buildUserRoute(), pathPrefix("admin", { buildAdminRoute() }))
            })
    )

    private fun ask(actor: ActorSelection, message: Any): CompletionStage<Any>
            = FutureConverters.toJava(Patterns.ask(actor, message, 5000))

    private fun json(obj: Any): Route {
        var json: String

        try {
            json = mapper.writeValueAsString(obj)
        } catch (e: JsonProcessingException) {
            json = mapper.writeValueAsString(e)
        }

        val response = HttpResponse.create()
                .withEntity(MediaTypes.APPLICATION_JSON.toContentType(), json)

        return complete(response)
    }
}