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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.prestosql.spi.eventlistener.EventListener;
import io.prestosql.spi.eventlistener.EventListenerFactory;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

import java.util.Map;

import static java.util.Objects.requireNonNull;

public class QueryLogListenerFactory implements EventListenerFactory {
    private static final String QUERYLOG_CONFIG_LOCATION = "presto.querylog.log4j2.configLocation";
    private static final String QUERYLOG_TRACK_CREATED = "presto.querylog.log.queryCreatedEvent";
    private static final String QUERYLOG_TRACK_COMPLETED = "presto.querylog.log.queryCompletedEvent";
    private static final String QUERYLOG_TRACK_COMPLETED_SPLIT = "presto.querylog.log.splitCompletedEvent";

    private static final String QUERYLOG_TRACK_COMPLETED_QUERY_LENGTH = "presto.querylog.log.queryCompletedEvent.queryLength";

    private static final String QUERYLOG_CONFIG_LOCATION_ERROR = QUERYLOG_CONFIG_LOCATION + " is null";

    @Override
    public String getName() {
        return "presto-querylog";
    }

    @Override
    public EventListener create(Map<String, String> map) {
        String log4j2ConfigLocation = requireNonNull(map.get(QUERYLOG_CONFIG_LOCATION), QUERYLOG_CONFIG_LOCATION_ERROR);
        LoggerContext loggerContext = Configurator.initialize("presto-querylog", log4j2ConfigLocation);
        boolean trackEventCreated = getBooleanConfig(map, QUERYLOG_TRACK_CREATED, true);
        boolean trackEventCompleted = getBooleanConfig(map, QUERYLOG_TRACK_COMPLETED, true);
        boolean trackEventCompletedSplit = getBooleanConfig(map, QUERYLOG_TRACK_COMPLETED_SPLIT, true);
        int trackEventCompletedQueryLength = getIntegerConfig(map, QUERYLOG_TRACK_COMPLETED_QUERY_LENGTH, -1);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        return new QueryLogListener(loggerContext, mapper, trackEventCreated, trackEventCompleted, trackEventCompletedSplit, trackEventCompletedQueryLength);
    }

    /**
     * Get {@code boolean} parameter value, or return default.
     *
     * @param params       Map of parameters
     * @param paramName    Parameter name
     * @param paramDefault Parameter default value
     * @return Parameter value or default.
     */
    private boolean getBooleanConfig(Map<String, String> params, String paramName, boolean paramDefault) {
        String value = params.get(paramName);
        if (value != null && !value.trim().isEmpty()) {
            return Boolean.parseBoolean(value);
        }
        return paramDefault;
    }

    /**
     * Get {@code int} parameter value, or return default.
     *
     * @param params       Map of parameters
     * @param paramName    Parameter name
     * @param paramDefault Parameter default value
     * @return Parameter value or default.
     */
    private int getIntegerConfig(Map<String, String> params, String paramName, int paramDefault) {
        String value = params.get(paramName);
        if (value != null && !value.trim().isEmpty()) {
            return Integer.parseInt(value);
        }
        return paramDefault;
    }
}
