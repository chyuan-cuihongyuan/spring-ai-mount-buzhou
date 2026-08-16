package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 54 §D / T226 / impl-187：Redis 限流后端红队面——
 * 并发扣减竞差（INCR 原子性 + 回滚净额，无超发无泄漏）/ 跨进程时区无关性
 * （窗口键 epoch 时基钉住）/ 断连全路径 fail-fast（不静默 fail-open）。
 * 真实 Redis 上的跨实例共享由 ContainersTest 在 CI 验证；此处 stub 并发安全
 * （ConcurrentHashMap.merge 原子语义 = 单线程 Redis 命令串行的等价保真）。
 */
class RedisRateLimitBackendRedteamTest {

    /** 并发安全 stub：ConcurrentHashMap.merge 原子读改写（对应 Redis 单线程命令串行）。 */
    @SuppressWarnings("unchecked")
    private static RedisCommands<String, String> stubCommands(
            ConcurrentHashMap<String, Long> store, Set<String> ttlSet) {
        InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
            String name = method.getName();
            String key = (String) args[0];
            return switch (name) {
                case "incrby" -> store.merge(key, (Long) args[1], Long::sum);
                case "decrby" -> store.merge(key, -(Long) args[1], Long::sum);
                case "get" -> {
                    Long v = store.get(key);
                    yield v == null ? null : String.valueOf(v);
                }
                case "expire" -> ttlSet.add(key) ? Boolean.TRUE : Boolean.TRUE;
                default -> null;
            };
        };
        return (RedisCommands<String, String>) Proxy.newProxyInstance(
                RedisCommands.class.getClassLoader(),
                new Class<?>[]{RedisCommands.class}, handler);
    }

    @SuppressWarnings("unchecked")
    private static RedisRateLimitBackend backend(
            ConcurrentHashMap<String, Long> store, Set<String> ttlSet) {
        StatefulRedisConnection<String, String> conn = (StatefulRedisConnection<String, String>)
                Proxy.newProxyInstance(StatefulRedisConnection.class.getClassLoader(),
                        new Class<?>[]{StatefulRedisConnection.class},
                        (proxy, method, args) -> stubCommands(store, ttlSet));
        return new RedisRateLimitBackend(conn, "buzhou:rl:", 4, 100);
    }

    /** 竞差攻击：容量 4、16 线程 × 各 8 次并发抢——放行恰 4 次（无超发），净计数恰 4（回滚无泄漏）。 */
    @Test
    void concurrentAcquireNeverOverAdmitsOrLeaksQuota() throws Exception {
        ConcurrentHashMap<String, Long> store = new ConcurrentHashMap<>();
        RedisRateLimitBackend b = backend(store, new CopyOnWriteArraySet<>());
        int threads = 16;
        int perThread = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger admitted = new AtomicInteger();
            List<Future<Void>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    for (int j = 0; j < perThread; j++) {
                        if (b.tryAcquire("race", "RPM", 1)) {
                            admitted.incrementAndGet();
                        }
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<Void> f : futures) {
                f.get(30, TimeUnit.SECONDS);
            }
            // 无超发：全局放行数 = 容量；无泄漏：窗口净计数 = 放行数（拒绝全部回滚干净）
            assertThat(admitted.get()).isEqualTo(4);
            assertThat(store.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(4);
            assertThat(b.available("race", "RPM")).isZero();
        } finally {
            pool.shutdownNow();
        }
    }

    /** 并发记账不丢：8 线程 × 各记 10 → 计数恰 80（merge 原子性 = INCR 原子性等价保真）。 */
    @Test
    void concurrentConsumeCountsExactly() throws Exception {
        ConcurrentHashMap<String, Long> store = new ConcurrentHashMap<>();
        RedisRateLimitBackend b = backend(store, new CopyOnWriteArraySet<>());
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Void>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    for (int j = 0; j < 10; j++) {
                        b.consume("acc", "TPM", 10);
                    }
                    return null;
                }));
            }
            for (Future<Void> f : futures) {
                f.get(30, TimeUnit.SECONDS);
            }
            assertThat(store.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(800);
            assertThat(b.available("acc", "TPM")).isZero(); // 100 - 800 → 负余额封顶 0
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * 跨进程时区/时钟无关性：窗口键 = epochMinute（epoch 时基）——任意默认 TimeZone 下
     * 同一分钟派生同一窗口号，无本地时间掺入（跨时区多实例共享同一窗）。
     */
    @Test
    void windowKeysAreEpochBasedAndTimezoneIndependent() {
        List<String> zones = List.of("UTC", "Asia/Shanghai", "America/New_York", "Pacific/Kiritimati");
        TimeZone original = TimeZone.getDefault();
        try {
            for (String zone : zones) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                long before = System.currentTimeMillis() / 60_000;
                ConcurrentHashMap<String, Long> store = new ConcurrentHashMap<>();
                RedisRateLimitBackend b = backend(store, new CopyOnWriteArraySet<>());
                assertThat(b.tryAcquire("tz", "RPM", 1)).isTrue();
                long after = System.currentTimeMillis() / 60_000;
                // 键存在且窗口号 ∈ [before, after]（epoch 派生，与默认时区无关）
                assertThat(store.keySet()).hasSize(1);
                String key = store.keySet().iterator().next();
                long window = Long.parseLong(key.substring(key.lastIndexOf(':') + 1));
                assertThat(window).isBetween(before, after);
                // 钉住语义：窗口号 = epochMillis/60000，非任何本地日历字段
                ZonedDateTime nowUtc = Instant.ofEpochMilli(window * 60_000L).atZone(ZoneId.of("UTC"));
                assertThat(nowUtc.getSecond()).isZero(); // 窗起点对齐整分（UTC 视角）
            }
        } finally {
            TimeZone.setDefault(original);
        }
    }

    /** 断连全路径 fail-fast：tryAcquire/consume/available 全 wrap 为 BuzhouException（不静默、不吞成放行/0）。 */
    @Test
    void everyStoragePathFailsFastUnderDisconnect() {
        StatefulRedisConnection<String, String> dead = deadConnection();
        RedisRateLimitBackend b = new RedisRateLimitBackend(dead, "buzhou:rl:", 4, 100);
        assertThatThrownBy(() -> b.tryAcquire("m", "RPM", 1))
                .isInstanceOf(BuzhouException.class)
                .hasMessageContaining("fail-fast")
                .hasMessageContaining("修法") // 带修法（runbook 可操作性）
                .hasCauseInstanceOf(RedisCommandExecutionException.class);
        assertThatThrownBy(() -> b.consume("m", "TPM", 5))
                .isInstanceOf(BuzhouException.class);
        assertThatThrownBy(() -> b.available("m", "TPM"))
                .isInstanceOf(BuzhouException.class);
        // 本地纯计算路径不受断连影响（等待提示永远可得，由策略层超时兜底）
        assertThat(b.secondsUntilAvailable("m", "RPM", 1)).isGreaterThan(0);
    }

    @SuppressWarnings("unchecked")
    private static StatefulRedisConnection<String, String> deadConnection() {
        return (StatefulRedisConnection<String, String>) Proxy.newProxyInstance(
                StatefulRedisConnection.class.getClassLoader(),
                new Class<?>[]{StatefulRedisConnection.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("sync")) {
                        return deadCommands();
                    }
                    return null;
                });
    }

    @SuppressWarnings("unchecked")
    private static RedisCommands<String, String> deadCommands() {
        return (RedisCommands<String, String>) Proxy.newProxyInstance(
                RedisCommands.class.getClassLoader(),
                new Class<?>[]{RedisCommands.class},
                (proxy, method, args) -> {
                    throw new RedisCommandExecutionException("connection refused (redteam)");
                });
    }
}
