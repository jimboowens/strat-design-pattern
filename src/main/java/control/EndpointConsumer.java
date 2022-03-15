package control;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;

@ApplicationScoped
@Path("/test")
public class EndpointConsumer {

    @Inject
    Logger log;

    public void consume(String body) {
        log.info("body: [" + body + "]");
    }

}
