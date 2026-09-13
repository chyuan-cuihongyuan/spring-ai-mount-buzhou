package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SessionStateStore 5 个 default 方法直测（spec 1200 / T1801 / K 会话 R1 补测）。
 *
 * <p>既有实现（InMemory/JDBC/Redis）全部覆写 default，接口 default 体被遮蔽成死路径——
 * 本测试用只实现 5 个抽象方法的<b>裸实现</b>驱动 default 体（SessionStateStoreContractTest
 * 匿名走样写法），断言 scanByKeyRange 边界、countByPrefix 前缀边界、compareAndSwap 与
 * scanByPrefix 语义。
 */
class SessionStateStoreDefaultsTest {

    private static final String SESSION = "s-defaults";
    private static final String PREFIX = "outbox.";

    private static StateEntry entry(String key, String value) {
        return new StateEntry(key, value, "test", 1, null, Instant.now());
    }

    /** 裸实现：只实现 5 个抽象方法，不覆写任何 default。 */
    private static final class BareSessionStateStore implements SessionStateStore {
        private final Map<String, Map<String, StateEntry>> sessions = new java.util.HashMap<>();

        private Map<String, StateEntry> session(String sessionId) {
            return sessions.computeIfAbsent(sessionId, k -> new LinkedHashMap<>());
        }

        @Override
        public void put(String sessionId, StateEntry entry) {
            session(sessionId).put(entry.key(), entry);
        }

        @Override
        public Optional<StateEntry> get(String sessionId, String key) {
            return Optional.ofNullable(session(sessionId).get(key));
        }

        @Override
        public Map<String, StateEntry> getAll(String sessionId) {
            return new LinkedHashMap<>(session(sessionId));
        }

        @Override
        public void delete(String sessionId, String key) {
            session(sessionId).remove(key);
        }

        @Override
        public boolean deleteIfValueMatches(String sessionId, String key, String expectedValue) {
            StateEntry current = session(sessionId).get(key);
            if (current == null || !current.value().equals(expectedValue)) {
                return false;
            }
            session(sessionId).remove(key);
            return true;
        }
    }

    private static BareSessionStateStore storeWithOutboxKeys() {
        BareSessionStateStore store = new BareSessionStateStore();
        store.put(SESSION, entry(PREFIX + "003", "c"));
        store.put(SESSION, entry(PREFIX + "001", "a"));
        store.put(SESSION, entry(PREFIX + "002", "b"));
        store.put(SESSION, entry("dead.001", "d"));
        store.put(SESSION, entry("outboxx", "e"));
        return store;
    }

    @Test
    void scanByPrefixReturnsOnlyMatchingKeysInInsertionOrder() {
        BareSessionStateStore store = storeWithOutboxKeys();
        Map<String, StateEntry> scanned = store.scanByPrefix(SESSION, PREFIX);
        assertThat(scanned).containsOnlyKeys(PREFIX + "003", PREFIX + "001", PREFIX + "002");
        // getAll 保序（LinkedHashMap）→ default 过滤结果保序
        assertThat(scanned.keySet()).containsExactly(PREFIX + "003", PREFIX + "001", PREFIX + "002");
    }

    @Test
    void countByPrefixCountsOnlyExactPrefixHits() {
        BareSessionStateStore store = storeWithOutboxKeys();
        assertThat(store.countByPrefix(SESSION, PREFIX)).isEqualTo(3);
        assertThat(store.countByPrefix(SESSION, "dead.")).isEqualTo(1);
        // "outboxx" 以 "outbox" 开头但不在 "outbox." 前缀内
        assertThat(store.countByPrefix(SESSION, "outboxx")).isEqualTo(1);
        assertThat(store.countByPrefix(SESSION, "nope.")).isZero();
    }

    @Test
    void scanByKeyRangeOrdersAscendingAndTruncatesByLimit() {
        BareSessionStateStore store = storeWithOutboxKeys();

        Map<String, StateEntry> all = store.scanByKeyRange(SESSION, PREFIX, null, null, 10);
        assertThat(all.keySet()).containsExactly(PREFIX + "001", PREFIX + "002", PREFIX + "003");

        Map<String, StateEntry> firstTwo = store.scanByKeyRange(SESSION, PREFIX, null, null, 2);
        assertThat(firstTwo.keySet()).containsExactly(PREFIX + "001", PREFIX + "002");
    }

    @Test
    void scanByKeyRangeHonorsBoundsAndNonPositiveLimit() {
        BareSessionStateStore store = storeWithOutboxKeys();

        // fromKeyInclusive 含界
        assertThat(store.scanByKeyRange(SESSION, PREFIX, PREFIX + "002", null, 10).keySet())
                .containsExactly(PREFIX + "002", PREFIX + "003");
        // toKeyExclusive 排他
        assertThat(store.scanByKeyRange(SESSION, PREFIX, null, PREFIX + "003", 10).keySet())
                .containsExactly(PREFIX + "001", PREFIX + "002");
        // 双界 + limit 截断
        assertThat(store.scanByKeyRange(SESSION, PREFIX, PREFIX + "001", PREFIX + "004", 1).keySet())
                .containsExactly(PREFIX + "001");
        // limit<=0 → 空结果
        assertThat(store.scanByKeyRange(SESSION, PREFIX, null, null, 0)).isEmpty();
        assertThat(store.scanByKeyRange(SESSION, PREFIX, null, null, -1)).isEmpty();
    }

    @Test
    void defaultCompareAndSwapGatesOnCurrentValue() {
        BareSessionStateStore store = new BareSessionStateStore();

        // expectedValue=null：键不存在才写（首写语义）
        assertThat(store.compareAndSwap(SESSION, "quota", null, entry("quota", "10"))).isTrue();
        assertThat(store.compareAndSwap(SESSION, "quota", null, entry("quota", "99"))).isFalse();
        assertThat(store.get(SESSION, "quota").orElseThrow().value()).isEqualTo("10");

        assertThat(store.compareAndSwap(SESSION, "quota", "10", entry("quota", "9"))).isTrue();
        assertThat(store.compareAndSwap(SESSION, "quota", "10", entry("quota", "8"))).isFalse();
        assertThat(store.get(SESSION, "quota").orElseThrow().value()).isEqualTo("9");
    }

    @Test
    void defaultDeleteSessionIsNoopKeepingEntries() {
        BareSessionStateStore store = storeWithOutboxKeys();
        store.deleteSession(SESSION);
        assertThat(store.getAll(SESSION)).isNotEmpty();
    }
}
