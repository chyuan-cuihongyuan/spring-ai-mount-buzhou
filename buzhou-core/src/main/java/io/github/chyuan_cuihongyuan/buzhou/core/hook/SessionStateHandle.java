package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import java.util.Optional;

public interface SessionStateHandle {

    <T> Optional<T> get(String key, Class<T> type);

    void put(String key, Object value);

    void delete(String key);
}
