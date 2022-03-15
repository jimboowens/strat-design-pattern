package control;

import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.LifecycleStrategySupport;

@ApplicationScoped
public class Routes extends RouteBuilder {

    @Inject
    Logger log;

    @Inject
    EndpointConsumer epc;

    private ReentrantLock lock = new ReentrantLock();

    private static final String deadLetterEndpoint = "seda:todo";

    @Override
    public void configure() throws Exception {
        errorHandler(deadLetterChannel(deadLetterEndpoint));
        getContext().addLifecycleStrategy(new InterceptShutdownStrategy());

        from("sjms:queue:default-route").id("default-route") // -
                .process(this::sendToEPC);
    }

    private void sendToEPC(Exchange exch) {
        lock.lock();
        try {
            epc.consume(exch.getMessage().getBody(String.class));
        } finally {
            lock.unlock();
        }

    }

    public class InterceptShutdownStrategy extends LifecycleStrategySupport {
        @Override
        public void onContextStopping(CamelContext context) {
            log.info("Stop Requested, trying to shut down...");
            try {
                context.getRouteController().stopRoute("default-route");
                log.info("Route(s) stopped!");
            } catch (Exception e) {
                e.printStackTrace();
            }
            super.onContextStopping(context);
        }
    }

}
