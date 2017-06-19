package http;

import akka.NotUsed;
import akka.actor.ActorContext;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.AllDirectives;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.concurrent.CompletionStage;

public class Router extends AllDirectives {

    private final CompletionStage<ServerBinding> binding;
    private final ActorContext context;
    private final ObjectMapper mapper = new ObjectMapper();

    public Router(ActorContext context) {
        Config config = ConfigFactory.load();

        this.context = context;

        final ActorSystem system = context.system();
        final RouteMapper mapper = new RouteMapper(this);

        Http http = Http.get(system);
        ActorMaterializer materializer = ActorMaterializer.create(system);
        Flow<HttpRequest, HttpResponse, NotUsed> flow = mapper.flow(system, materializer);
        binding = http.bindAndHandle(
                flow,
                ConnectHttp.toHost(config.getString("http.ip"), config.getInt("http.port")),
                materializer
        );
    }

    public void shutdown() {
        binding.thenCompose(ServerBinding::unbind);
    }

    public ActorContext getContext() {
        return this.context;
    }

    public ObjectMapper getMapper() {
        return this.mapper;
    }
}
