package com.github.presto.querylog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.airlift.units.DataSize;
import io.prestosql.spi.eventlistener.QueryContext;
import io.prestosql.spi.eventlistener.QueryCreatedEvent;
import io.prestosql.spi.eventlistener.QueryMetadata;
import io.prestosql.spi.eventlistener.SplitCompletedEvent;
import io.prestosql.spi.eventlistener.SplitStatistics;
import io.prestosql.spi.session.ResourceEstimates;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;

// Those are just a few very crude tests.
// TODO: Add more cases with proper structure.
// TODO: Test actual JSON output, not just its presence.
class QueryLogListenerTest {

    static ObjectMapper mapper;

    @BeforeAll
    static void setup() {
        mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
    }

    @Test
    void shrinkPrestoQuery() {
        String s = "123456789012345678901234567890";
        StringBuilder sb = new StringBuilder();
        sb.append(s, 0, 10);
        sb.append("....");
        sb.append(s, s.length()-10, s.length());
        System.out.println(sb.toString());
        System.out.println(sb.length());
    }

    @Test
    void dateFormat() {
        Instant i = Instant.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String r = formatter.format(LocalDateTime.ofInstant(i, ZoneId.of("UTC")));
        System.out.println(r);
    }

    @Test
    void queryCreatedEvents() throws IOException {
        LoggerContext loggerContext = Configurator.initialize(
                "queryCreatedEvents",
                "classpath:queryCreatedEvents.xml"
        );
        try {
            // Given there is a listener for query created event
            QueryLogListener listener = new QueryLogListener(
                    loggerContext, mapper,
                    true, true, true, -1
            );

            // When two events are created
            listener.queryCreated(prepareQueryCreatedEvent());
            listener.queryCreated(prepareQueryCreatedEvent());

            // Then two events should be present in the log file
            long logEventsCount = Files.lines(Paths.get("target/queryCreatedEvents.log")).count();
            assertEquals(2, logEventsCount);
        } finally {
            Configurator.shutdown(loggerContext);
        }
    }


    @Test
    void onlyQueryCreatedEvents() throws IOException {
        LoggerContext loggerContext = Configurator.initialize(
                "onlyQueryCreatedEvents",
                "classpath:onlyQueryCreatedEvents.xml"
        );
        try {
            // Given there is a listener for query created event
            QueryLogListener listener = new QueryLogListener(
                    loggerContext, mapper,
                    true, false, false, -1
            );

            // When one created event is created
            //  And one split completed event is created
            listener.queryCreated(prepareQueryCreatedEvent());
            listener.splitCompleted(prepareSplitCompletedEvent());

            // Then only created event should be present in the log file
            long logEventsCount = Files.lines(Paths.get("target/onlyQueryCreatedEvents.log")).count();
            assertEquals(1, logEventsCount);
        } finally {
            Configurator.shutdown(loggerContext);
        }
    }

    private QueryCreatedEvent prepareQueryCreatedEvent() {
        return new QueryCreatedEvent(
                Instant.now(),
                prepareQueryContext(),
                prepareQueryMetadata()
        );
    }

    private SplitCompletedEvent prepareSplitCompletedEvent() {
        return new SplitCompletedEvent(
                "queryId",
                "stageId",
                "taskId",
                Instant.now(),
                Optional.of(Instant.now()),
                Optional.of(Instant.now()),
                getSplitStatistics(),
                Optional.empty(),
                "payload"
        );
    }

    private SplitStatistics getSplitStatistics() {
        return new SplitStatistics(
                ofMillis(1000),
                ofMillis(2000),
                ofMillis(3000),
                ofMillis(4000),
                1,
                2,
                Optional.of(Duration.ofMillis(100)),
                Optional.of(Duration.ofMillis(200))
        );
    }

    private QueryMetadata prepareQueryMetadata() {
        return new QueryMetadata(
                "queryId", Optional.empty(),
                "query",
                Optional.of("preparedQuery"),
                "queryState",
                URI.create("http://localhost"),
                Optional.empty(), Optional.empty()
        );
    }

    private QueryContext prepareQueryContext() {
        return new QueryContext(
                "user",
                Optional.of("principal"),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), new HashSet<String>(), new HashSet<String>(), Optional.empty(), Optional.empty(),
                Optional.empty(),Optional.empty(),new HashMap<>(),
                new ResourceEstimates(Optional.empty(), Optional.empty(), Optional.of(DataSize.succinctDataSize(1000, DataSize.Unit.BYTE))),
                "serverAddress", "serverVersion", "environment"
        );
    }
}
