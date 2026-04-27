package ru.practicum.iteractionapi.statistics;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface StatisticsService {
    Map<String, Long> getViewsByUris(List<String> uris, boolean unique);

    Long getViewsByUri(String uri, boolean unique);

    Map<String, Long> getEventShortDto(Set<Long> events, boolean unique);
}
