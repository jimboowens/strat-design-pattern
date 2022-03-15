package control;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

import java.net.URL;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.component.sjms.SjmsComponent;
import org.apache.camel.quarkus.core.events.ComponentAddEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import io.agroal.api.AgroalDataSource;

/**
 * Global Bootstrapper
 */
@ApplicationScoped
public class GlobalConfig {

    private static final Logger LOG = Logger.getLogger(GlobalConfig.class.getCanonicalName());

    @Inject
    @ConfigProperty(name = "AMQ_URI")
    String amqUri;

    @Inject
    AgroalDataSource ds;

    @PostConstruct
    public void pc() {
        LOG.info("here");
        var omJson = new ObjectMapper();
        omJson.registerModule(new JavaTimeModule());
        omJson.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        Configuration.setDefaults(new Configuration.Defaults() {
            private final JacksonJsonProvider jsonProvider = new JacksonJsonProvider(omJson);
            private final JacksonMappingProvider mappingProvider = new JacksonMappingProvider(omJson);

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider();
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider();
            }

            @Override
            public Set<Option> options() {
                return EnumSet.noneOf(Option.class);
            }
        });
    }

    @Produces
    public NamedParameterJdbcTemplate getSpecTemplate() {
        return new NamedParameterJdbcTemplate(ds);
    }

    @Dependent
    @Produces
    public Logger produceLog(InjectionPoint ip) {
        return Logger.getLogger(ip.getMember().getDeclaringClass().getName());
    }

    /**
     * Avoid muddling files with complex Sql Strings.
     * Just put them in a sql file and import them
     */
    @Produces
    @SqlFile
    public String getStringFromFile(InjectionPoint ip) throws IOException {
        Class<?> clazz = ip.getMember().getDeclaringClass();
        String name = ip.getMember().getName();
        URL destination = clazz.getResource(name + ".sql");

        if (destination == null) {
            throw new IllegalArgumentException(
                    String.format("Could not resovle reference to [%s.sql] at clazz [%s]", name, clazz));
        }
        StringBuilder sb = new StringBuilder();
        try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(destination.openStream()))) {
            String line = reader.readLine();
            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = reader.readLine();
            }
        }
        return sb.toString();
    }

    // configure AMQ
    public void onComponentAdd(@Observes ComponentAddEvent cAddEvent) {
        if (cAddEvent.getComponent() instanceof SjmsComponent) {
            SjmsComponent component = (SjmsComponent) cAddEvent.getComponent();
            LOG.info("creating amq config with URI: [" + amqUri + "]");
            component.setConnectionFactory(new ActiveMQConnectionFactory(amqUri));
        }
    }
}
