package io.github.chyuan_cuihongyuan.buzhou.spill;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SessionReadOnlyRegistry {

    private final Map<String, Set<Path>> readOnlyBySession = new ConcurrentHashMap<>();

    public void register(String sessionId, Path path) {
        readOnlyBySession.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                .add(normalize(path));
    }

    public boolean isReadOnly(String sessionId, Path path) {
        Set<Path> paths = readOnlyBySession.get(sessionId);
        return paths != null && paths.contains(normalize(path));
    }

    public void evict(String sessionId) {
        readOnlyBySession.remove(sessionId);
    }

    static Path normalize(Path path) {
        try {
            if (Files.exists(path)) {
                return path.toRealPath();
            }
        } catch (IOException ignored) {
        }
        return path.toAbsolutePath().normalize();
    }
}
