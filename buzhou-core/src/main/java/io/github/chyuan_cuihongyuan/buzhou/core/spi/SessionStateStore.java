package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.util.Map;
import java.util.Optional;

public interface SessionStateStore {

    void put(String sessionId, StateEntry entry);

    Optional<StateEntry> get(String sessionId, String key);

    Map<String, StateEntry> getAll(String sessionId);

    void delete(String sessionId, String key);
}
