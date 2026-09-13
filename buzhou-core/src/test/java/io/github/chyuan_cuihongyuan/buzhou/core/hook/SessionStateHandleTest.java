package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SessionStateHandle default compareAndSwap 直测（spec 1200 / T1801 / K 会话 R1 补测——
 * 此前零覆盖）。
 *
 * <p>匿名内存实现只覆写 3 个抽象方法、驱动 default CAS 体：匹配覆写 / 不匹配不动 /
 * null expected 首写语义 / 非字符串值的 String 口径比对边界（分层诚实边界防语义漂移）。
 */
class SessionStateHandleTest {

    /** 内存 SessionStateHandle：get 按 type 实例判定（复现 String 口径比对语义）。 */
    private static final class InMemoryStateHandle implements SessionStateHandle {
        private final Map<String, Object> values = new HashMap<>();

        @Override
        public <T> Optional<T> get(String key, Class<T> type) {
            Object value = values.get(key);
            return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
        }

        @Override
        public void put(String key, Object value) {
            values.put(key, value);
        }

        @Override
        public void delete(String key) {
            values.remove(key);
        }
    }

    @Test
    void casOverwritesOnlyWhenExpectedMatches() {
        InMemoryStateHandle handle = new InMemoryStateHandle();
        handle.put("quota", "10");

        assertThat(handle.compareAndSwap("quota", "10", "9")).isTrue();
        assertThat(handle.get("quota", String.class)).contains("9");

        assertThat(handle.compareAndSwap("quota", "10", "8")).isFalse();
        assertThat(handle.get("quota", String.class)).contains("9");
    }

    @Test
    void casWithNullExpectedMeansCreateOnly() {
        InMemoryStateHandle handle = new InMemoryStateHandle();
        assertThat(handle.compareAndSwap("lock", null, "owner-1")).isTrue();
        assertThat(handle.get("lock", String.class)).contains("owner-1");

        assertThat(handle.compareAndSwap("lock", null, "owner-2")).isFalse();
        assertThat(handle.get("lock", String.class)).contains("owner-1");
    }

    @Test
    void casComparesThroughStringLensOnly() {
        // 文档性断言：default 实现以 get(key, String.class) 比对——非 String 值比对落空
        InMemoryStateHandle handle = new InMemoryStateHandle();
        handle.put("n", 42);
        assertThat(handle.compareAndSwap("n", "42", "43")).isFalse();
        // null expected 视作「键不存在」（String 口径下 Integer 值不可见）→ 覆写成功
        assertThat(handle.compareAndSwap("n", null, "43")).isTrue();
        assertThat(handle.get("n", String.class)).contains("43");
    }

    @Test
    void deleteRemovesKeyEnablingCreateAgain() {
        InMemoryStateHandle handle = new InMemoryStateHandle();
        handle.put("k", "v");
        handle.delete("k");
        assertThat(handle.get("k", String.class)).isEmpty();
        assertThat(handle.compareAndSwap("k", null, "v2")).isTrue();
    }
}
