package io.github.chyuan_cuihongyuan.buzhou.core.prompt;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 进程内提示词注册表（spec 401 / T693）：per-name 单调版本 + 标签指针。
 * 跨实例共享（DB/Redis）为诚实边界——真需求出现再扩 SPI。
 */
public final class InMemoryPromptRegistry implements PromptRegistry {

    private static final class NameState {
        final AtomicInteger nextVersion = new AtomicInteger(1);
        final List<PromptVersion> versions = new ArrayList<>();
        final Map<String, Integer> labels = new ConcurrentHashMap<>();
    }

    private final Map<String, NameState> states = new ConcurrentHashMap<>();

    @Override
    public PromptVersion publish(String name, String body, String note) {
        requireName(name);
        NameState state = states.computeIfAbsent(name, n -> new NameState());
        synchronized (state) {
            PromptVersion v = new PromptVersion(name, state.nextVersion.getAndIncrement(),
                    body, note, Instant.now());
            state.versions.add(v);
            state.labels.put(LATEST, v.version());
            return v;
        }
    }

    @Override
    public void label(String name, String label, int version) {
        requireName(name);
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label 不可为空（name=" + name + "）");
        }
        NameState state = states.get(name);
        if (state == null) {
            throw new IllegalArgumentException("未知提示词名（name=" + name + "）——先 publish");
        }
        synchronized (state) {
            boolean exists = state.versions.stream().anyMatch(v -> v.version() == version);
            if (!exists) {
                throw new IllegalArgumentException("未知版本（name=" + name
                        + ", version=" + version + "）——现存 "
                        + state.versions.size() + " 版");
            }
            state.labels.put(label, version);
        }
    }

    @Override
    public Optional<PromptVersion> resolve(String name) {
        return resolve(name, LATEST);
    }

    @Override
    public Optional<PromptVersion> resolve(String name, String label) {
        NameState state = name == null ? null : states.get(name);
        if (state == null || label == null) {
            return Optional.empty();
        }
        Integer version = state.labels.get(label);
        return version == null ? Optional.empty() : resolveVersion(name, version);
    }

    @Override
    public Optional<PromptVersion> resolveVersion(String name, int version) {
        NameState state = name == null ? null : states.get(name);
        if (state == null) {
            return Optional.empty();
        }
        synchronized (state) {
            return state.versions.stream()
                    .filter(v -> v.version() == version)
                    .findFirst();
        }
    }

    @Override
    public Map<String, Integer> labels(String name) {
        NameState state = name == null ? null : states.get(name);
        return state == null ? Map.of() : Map.copyOf(state.labels);
    }

    @Override
    public List<PromptVersion> versions(String name) {
        NameState state = name == null ? null : states.get(name);
        if (state == null) {
            return List.of();
        }
        synchronized (state) {
            return List.copyOf(state.versions);
        }
    }

    @Override
    public Set<String> names() {
        return Set.copyOf(states.keySet());
    }

    private static void requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("提示词名不可为空");
        }
    }
}
