/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.presto.querylog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.prestosql.spi.eventlistener.EventListener;
import io.prestosql.spi.eventlistener.QueryCompletedEvent;
import io.prestosql.spi.eventlistener.QueryCreatedEvent;
import io.prestosql.spi.eventlistener.SplitCompletedEvent;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

public class QueryLogListener implements EventListener {
    private final Logger logger;
    private final ObjectMapper mapper;
    private final boolean trackEventCreated;
    private final boolean trackEventCompleted;
    private final boolean trackEventCompletedSplit;
    private final int trackEventCompletedQueryLength;

    public QueryLogListener(final LoggerContext loggerContext,
                            final ObjectMapper mapper,
                            final boolean trackEventCreated,
                            final boolean trackEventCompleted,
                            final boolean trackEventCompletedSplit,
                            final int trackEventCompletedQueryLength) {
        this.trackEventCreated = trackEventCreated;
        this.mapper = mapper;
        this.trackEventCompleted = trackEventCompleted;
        this.trackEventCompletedSplit = trackEventCompletedSplit;
        this.logger = loggerContext.getLogger(QueryLogListener.class.getName());
        this.trackEventCompletedQueryLength = trackEventCompletedQueryLength;
    }

    @Override
    public void queryCreated(final QueryCreatedEvent queryCreatedEvent) {
        if (trackEventCreated) {
            try {
                logger.info(mapper.writeValueAsString(queryCreatedEvent));
            } catch (JsonProcessingException ignored) {
            }
        }
    }

    @Override
    public void queryCompleted(final QueryCompletedEvent queryCompletedEvent) {
        if (trackEventCompleted && !queryCompletedEvent.getIoMetadata().getInputs().get(0).getCatalogName().equals("$system@system")) {
            try {
                logger.info(mapper.writeValueAsString(new CustomLogContext().parse(queryCompletedEvent, trackEventCompletedQueryLength)));
            } catch (JsonProcessingException ignored) {
            }
        }
    }

    @Override
    public void splitCompleted(final SplitCompletedEvent splitCompletedEvent) {
        if (trackEventCompletedSplit) {
            try {
                logger.info(mapper.writeValueAsString(splitCompletedEvent));
            } catch (JsonProcessingException ignored) {
            }
        }
    }
}
