package trace;

import io.muserver.ContentTypes;
import io.muserver.Method;
import io.muserver.MuServer;
import j2html.tags.ContainerTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Stream;

import static io.muserver.ContextHandlerBuilder.context;
import static io.muserver.MuServerBuilder.muServer;
import static io.muserver.handlers.ResourceHandlerBuilder.fileOrClasspath;
import static j2html.TagCreator.*;

public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws Exception {
        Map<String, String> settings = System.getenv();

        // When run from app-runner, you must use the port set in the environment variable APP_PORT
        int port = Integer.parseInt(settings.getOrDefault("APP_PORT", "4227"));
        // All URLs must be prefixed with the app name, which is got via the APP_NAME env var.
        String appName = settings.getOrDefault("APP_NAME", "trace");
        log.info("Starting " + appName + " on port " + port);

        MuServer server = muServer()
            .withHttpPort(port)
            .addHandler(
                context(appName)
                    .addHandler(null, "/", (req, resp, params) -> {
                        resp.contentType(ContentTypes.TEXT_HTML);

                        html(
                            head(
                                title("Trace - see your request info"),
                                link().withRel("stylesheet").withHref("/trace/styles.css")
                            ),
                            body(
                                j2html.TagCreator.main(
                                    h1("Trace"),
                                    p("Use this to see the request info sent to the server."),
                                    p( text("Recieved "), strong(req.method().name()), text(" "), code(req.uri().getPath()), text("with querystring: "), code(req.uri().getQuery())),
                                    table(
                                        thead(tr(th("Request header"), th("Value"))),
                                        tbody(
                                            Stream.generate(req.headers().iterator()::next)
                                                .limit(req.headers().size())
                                                .map(entry ->
                                                    tr(
                                                        td(attrs(".headerName"), entry.getKey()),
                                                        td(attrs(".headerValue"), entry.getValue())
                                                    ))
                                                .toArray(ContainerTag[]::new)
                                        )
                                    )
                                )
                            )
                        ).render(resp.writer());


                    })
                    .addHandler(fileOrClasspath("src/main/resources/web", "/web"))
            )
            .start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        log.info("Started app at " + server.uri().resolve("/" + appName));
    }

}