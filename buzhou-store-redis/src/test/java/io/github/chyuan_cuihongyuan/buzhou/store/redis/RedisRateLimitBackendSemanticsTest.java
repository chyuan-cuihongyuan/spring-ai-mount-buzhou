package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 54 §B / T223：固定窗语义单元级（零新依赖——JDK 动态代理 stub；跨实例真实共享由
 * RedisRateLimitBackendContainersTest 在 CI 验证）——回滚/首写 TTL/超限/预检/故障语义/键净化。
 */
class RedisRateLimitBackendSemanticsTest {

    /** 进程内 stub（map 计数 + TTL 集合 + 可注入故障）；INCRLY/DECRLY/GET/EXPIRE 语义最小实现。 */
    @SuppressWarnings("unchecked")
    private static RedisCommands<String, String> stubCommands(
            Map<String, Long> store, Set<String> ttlSet, Function<String, ? extends RuntimeException> failure) {
        InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
            String name = method.getName();
            if (failure != null) {
                RuntimeException ex = failure.apply(name);
                if (ex != null) {
                    throw ex;
                }
            }
            String key = (String) args[0];
            return switch (name) {
                case "incrby" -> store.merge(key, (Long) args[1], Long::sum);
                case "decrby" -> store.merge(key, -(Long) args[1], Long::sum);
                case "get" -> {
                    Long v = store.get(key);
                    yield v == null ? null : String.valueOf(v);
                }
                case "expire" -> {
                    ttlSet.add(key);
                    yield true;
                }
                default -> null;
            };
        };
        return (RedisCommands<String, String>) Proxy.newProxyInstance(
                RedisCommands.class.getClassLoader(),
                new Class<?>[]{RedisCommands.class}, handler);
    }

    @SuppressWarnings("unchecked")
    private static RedisRateLimitBackend backend(Map<String, Long> store, Set<String> ttl) {
        return backend(store, ttl, null);
    }

    @SuppressWarnings("unchecked")
    private static RedisRateLimitBackend backend(Map<String, Long> store, Set<String> ttl,
            Function<String, ? extends RuntimeException> failure) {
        StatefulRedisConnection<String, String> conn = (StatefulRedisConnection<String, String>)
                Proxy.newProxyInstance(StatefulRedisConnection.class.getClassLoader(),
                        new Class<?>[]{StatefulRedisConnection.class},
                        (proxy, method, args) -> stubCommands(store, ttl, failure));
        return new RedisRateLimitBackend(conn, "buzhou:rl:", 4, 100);
    }

    @Test
    void incrThenRejectRollsBackAndSetsTtlOnFirstWrite() {
        Map<String, Long> store = new HashMap<>();
        Set<String> ttl = new HashSet<>();
        RedisRateLimitBackend b = backend(store, ttl);

        assertThat(b.tryAcquire("m", "RPM", 1)).isTrue();
        assertThat(b.tryAcquire("m", "RPM", 1)).isTrue();
        assertThat(b.tryAcquire("m", "RPM", 1)).isTrue();
        assertThat(b.tryAcquire("m", "RPM", 1)).isTrue();
        // 第 5 次超限回滚（净计数仍 4——不泄漏额度）
        assertThat(b.tryAcquire("m", "RPM", 1)).isFalse();
        assertThat(store.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(4);
        assertThat(b.available("m", "RPM")).isZero();
        // 首写窗口键已设 TTL
        assertThat(ttl).isNotEmpty();
    }

    @Test
    void tpmAccountingAllowsNegativeAndPreviewRejects() {
        Map<String, Long> store = new HashMap<>();
        RedisRateLimitBackend b = backend(store, new HashSet<>());
        b.consume("m", "TPM", 80);
        b.consume("m", "TPM", 40); // 超 100（诚实负余额）
        assertThat(b.available("m", "TPM")).isZero();
        assertThat(b.tryAcquire("m", "TPM", 0)).isFalse(); // 预检拒绝
        assertThat(b.tryAcquire("m", "RPM", 1)).isTrue(); // RPM 独立维度
    }

    @Test
    void redisFailuresSurfaceAsBuzhouExceptionFailFast() {
        Map<String, Long> store = new HashMap<>();
        RedisRateLimitBackend b = backend(store, new HashSet<>(),
                op -> new RedisCommandExecutionException("down on " + op));
        assertThatThrownBy(() -> b.tryAcquire("m", "RPM", 1))
                .isInstanceOf(BuzhouException.class)
                .hasMessageContaining("fail-fast");
        assertThatThrownBy(() -> b.available("m", "TPM"))
                .isInstanceOf(BuzhouException.class);
    }

    @Test
    void keySanitizesModelNameAndKindExposed() {
        Map<String, Long> store = new HashMap<>();
        RedisRateLimitBackend b = backend(store, new HashSet<>());
        assertThat(b.kind()).isEqualTo("redis");
        assertThat(b.capacity("RPM")).isEqualTo(4);
        assertThat(b.capacity("TPM")).isEqualTo(100);
        assertThat(b.capacity("OTHER")).isZero();
        // 模型名净化（冒号/斜杠/空格 → _）：键结构不可注入
        assertThat(b.tryAcquire("a:b/c d", "RPM", 1)).isTrue();
        assertThat(store.keySet()).allSatisfy(k -> assertThat(k)
                .doesNotContain(" ")
                .doesNotContain("a:")
                .doesNotContain("/"));
    }
}
