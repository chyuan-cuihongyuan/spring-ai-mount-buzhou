package io.github.chyuan_cuihongyuan.buzhou.core.fact;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 进程内共享事实库（spec 410 / T712）：ConcurrentHashMap + 读者集；
 * Clock 可注入（ttl 过期测试确定性）。持久化后端为扩散候选（诚实边界：
 * 进程内重启清零）。
 */
public final class InMemorySharedFactStore implements SharedFactStore {

    private static final class Entry {
        volatile SharedFact fact;
        final Set<String> readers = ConcurrentHashMap.newKeySet();
    }

    private final Map<String, Entry> facts = new ConcurrentHashMap<>();
    private final Clock clock;
    private final AtomicLong denied = new AtomicLong();

    public InMemorySharedFactStore() {
        this(Clock.systemUTC());
    }

    public InMemorySharedFactStore(Clock clock) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    public void publish(SharedFact fact) {
        if (fact == null) {
            throw new IllegalArgumentException("fact 非空");
        }
        facts.compute(fact.key(), (k, existing) -> {
            if (existing != null && !existing.fact.owner().equals(fact.owner())) {
                throw new IllegalArgumentException("键「" + k + "」归 " + existing.fact.owner()
                        + " 所有——非 owner 不得发布（键即所有权）");
            }
            Entry entry = existing == null ? new Entry() : existing;
            entry.fact = fact;
            return entry;
        });
    }

    @Override
    public void grant(String factKey, String reader) {
        requireKey(factKey);
        requireReader(reader);
        Entry entry = facts.get(factKey);
        if (entry == null) {
            throw new IllegalArgumentException("键「" + factKey + "」不存在——先 publish");
        }
        entry.readers.add(reader);
    }

    @Override
    public void revoke(String factKey, String reader) {
        Entry entry = factKey == null ? null : facts.get(factKey);
        if (entry != null && reader != null && !reader.equals(entry.fact.owner())) {
            entry.readers.remove(reader); // 幂等；owner 恒读——revoke owner 无效
        }
    }

    @Override
    public Optional<Object> read(String reader, String factKey) {
        Entry entry = factKey == null || reader == null ? null : facts.get(factKey);
        if (entry == null) {
            return Optional.empty();
        }
        if (!reader.equals(entry.fact.owner()) && !entry.readers.contains(reader)) {
            denied.incrementAndGet();
            BuzhouMetricsHolder.metrics().counter("buzhou.facts.denied-reads");
            return Optional.empty(); // deny-by-default
        }
        if (expired(entry.fact)) {
            return Optional.empty();
        }
        return Optional.ofNullable(entry.fact.value());
    }

    @Override
    public List<SharedFact> readable(String reader) {
        List<SharedFact> out = new ArrayList<>();
        if (reader == null) {
            return out;
        }
        Instant now = clock.instant();
        for (Entry entry : facts.values()) {
            boolean allowed = reader.equals(entry.fact.owner())
                    || entry.readers.contains(reader);
            if (allowed && !expired(entry.fact, now)) {
                out.add(entry.fact);
            }
        }
        return List.copyOf(out);
    }

    @Override
    public long deniedReads() {
        return denied.get();
    }

    private boolean expired(SharedFact fact) {
        return expired(fact, clock.instant());
    }

    private static boolean expired(SharedFact fact, Instant now) {
        return fact.ttl() != null && now.isAfter(fact.createdAt().plus(fact.ttl()));
    }

    private static void requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("factKey 非空");
        }
    }

    private static void requireReader(String reader) {
        if (reader == null || reader.isBlank()) {
            throw new IllegalArgumentException("reader 非空");
        }
    }
}
