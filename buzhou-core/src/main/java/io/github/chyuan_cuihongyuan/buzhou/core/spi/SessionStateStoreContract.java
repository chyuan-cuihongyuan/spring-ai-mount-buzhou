package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * SessionStateStore SPI 契约校验套件（spec 705 / T961，Pact consumer contract
 * testing 借鉴）：九项有序契约检查验证实现的语义面——CAS 消费一次（HITL 一次性
 * 放行依赖）、null-expect 首写（日翻越竞态钉住依赖）、前缀扫描、幂等清场等。
 * 第三方 store 实现一行调用即可自证，或启动诊断直接消费 {@link Report}。
 *
 * <p><b>主源码零测试依赖</b>：不用 JUnit——失败逐项收集不抛异常（一跑看全部
 * 缺口）。检查会话用 {@value #CONTRACT_SESSION_PREFIX} 前缀唯一 id，结束
 * {@code deleteSession} 自清理（对真实存储零残留）。
 */
public final class SessionStateStoreContract {

    /** 契约检查会话前缀（实现方据此识别可安全清理的探针数据）。 */
    public static final String CONTRACT_SESSION_PREFIX = "__contract__";

    /** 单项检查结果（不可变）。 */
    public record Check(String name, boolean passed, String detail) {
    }

    /** 契约报告（不可变；有序）。 */
    public record Report(List<Check> checks) {

        /** 全绿布尔。 */
        public boolean passed() {
            return checks.stream().allMatch(Check::passed);
        }

        /** 失败项名清单（全绿 = 空）。 */
        public List<String> failures() {
            return checks.stream().filter(c -> !c.passed()).map(Check::name).toList();
        }
    }

    private SessionStateStoreContract() {
    }

    /** 对 store 跑全部契约检查（结束自清理探针会话）。 */
    public static Report verify(SessionStateStore store) {
        if (store == null) {
            throw new IllegalArgumentException("store 必须非空");
        }
        String session = CONTRACT_SESSION_PREFIX + UUID.randomUUID();
        List<Check> checks = new ArrayList<>();
        try {
            checks.add(run("put-get-roundtrip", () -> putGetRoundtrip(store, session)));
            checks.add(run("get-all-returns-all", () -> getAllReturnsAll(store, session)));
            checks.add(run("delete-removes-key", () -> deleteRemoves(store, session)));
            checks.add(run("unknown-session-empty", () -> unknownSessionEmpty(store)));
            checks.add(run("delete-if-value-matches-consumes-once",
                    () -> deleteIfValueMatchesConsumesOnce(store, session)));
            checks.add(run("cas-null-expect-writes-only-absent",
                    () -> casNullExpectWritesOnlyAbsent(store, session)));
            checks.add(run("cas-match-overwrites", () -> casMatchOverwrites(store, session)));
            checks.add(run("delete-session-idempotent",
                    () -> deleteSessionIdempotent(store, session)));
            checks.add(run("scan-by-prefix-filters", () -> scanByPrefixFilters(store, session)));
        } finally {
            try {
                store.deleteSession(session);
            } catch (RuntimeException ignored) {
                // 清理失败不影响报告——探针会话由实现方按前缀识别清理
            }
        }
        return new Report(List.copyOf(checks));
    }

    private interface CheckBody {
        void run() throws Exception;
    }

    private static Check run(String name, CheckBody body) {
        try {
            body.run();
            return new Check(name, true, "ok");
        } catch (RuntimeException | AssertionError e) {
            return new Check(name, false, e.getClass().getSimpleName()
                    + (e.getMessage() == null ? "" : ": " + e.getMessage()));
        } catch (Exception e) {
            return new Check(name, false, "unexpected checked: " + e);
        }
    }

    private static void expect(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static StateEntry entry(String key, String value) {
        return new StateEntry(key, value, "contract", 0, null, Instant.now());
    }

    private static void putGetRoundtrip(SessionStateStore store, String session) {
        store.put(session, entry("rt.k", "v1"));
        expect(store.get(session, "rt.k").map(StateEntry::value).orElse(null).equals("v1"),
                "put 后 get 应读到同值");
    }

    private static void getAllReturnsAll(SessionStateStore store, String session) {
        store.put(session, entry("all.k1", "a"));
        store.put(session, entry("all.k2", "b"));
        var all = store.getAll(session);
        expect(all.size() >= 2 && all.containsKey("all.k1") && all.containsKey("all.k2"),
                "getAll 应含刚写入的全部键（实际 " + all.keySet() + "）");
    }

    private static void deleteRemoves(SessionStateStore store, String session) {
        store.put(session, entry("del.k", "x"));
        store.delete(session, "del.k");
        expect(store.get(session, "del.k").isEmpty(), "delete 后 get 应为空");
    }

    private static void unknownSessionEmpty(SessionStateStore store) {
        String ghost = CONTRACT_SESSION_PREFIX + "ghost-" + UUID.randomUUID();
        expect(store.get(ghost, "nope").isEmpty(), "未知会话 get 应为空");
        expect(store.getAll(ghost).isEmpty(), "未知会话 getAll 应为空");
    }

    private static void deleteIfValueMatchesConsumesOnce(SessionStateStore store, String session) {
        store.put(session, entry("cas-del.k", "v5"));
        expect(!store.deleteIfValueMatches(session, "cas-del.k", "wrong"),
                "值不匹配时 deleteIfValueMatches 应返回 false");
        expect("v5".equals(store.get(session, "cas-del.k").map(StateEntry::value).orElse(null)),
                "值不匹配删除后条目应仍在");
        expect(store.deleteIfValueMatches(session, "cas-del.k", "v5"),
                "值匹配应删除成功（放行消费一次）");
        expect(store.get(session, "cas-del.k").isEmpty(), "消费后条目应消失");
        expect(!store.deleteIfValueMatches(session, "cas-del.k", "v5"),
                "第二次删除应失败（一次性语义）");
    }

    private static void casNullExpectWritesOnlyAbsent(SessionStateStore store, String session) {
        expect(store.compareAndSwap(session, "cas-ne.k", null, entry("cas-ne.k", "first")),
                "expectedValue=null 且键缺位应写入成功");
        expect(!store.compareAndSwap(session, "cas-ne.k", null, entry("cas-ne.k", "second")),
                "键已存在时 null-expect 第二次应失败");
        expect("first".equals(store.get(session, "cas-ne.k").map(StateEntry::value).orElse(null)),
                "失败的 null-expect 不应覆写");
    }

    private static void casMatchOverwrites(SessionStateStore store, String session) {
        store.put(session, entry("cas-ow.k", "a"));
        expect(store.compareAndSwap(session, "cas-ow.k", "a", entry("cas-ow.k", "b")),
                "匹配值 CAS 应成功");
        expect(Objects.equals(store.get(session, "cas-ow.k").map(StateEntry::value).orElse(null), "b"),
                "匹配 CAS 后应读到新值");
        expect(!store.compareAndSwap(session, "cas-ow.k", "wrong", entry("cas-ow.k", "c")),
                "不匹配 CAS 应失败");
        expect(Objects.equals(store.get(session, "cas-ow.k").map(StateEntry::value).orElse(null), "b"),
                "失败 CAS 不应覆写");
    }

    private static void deleteSessionIdempotent(SessionStateStore store, String session) {
        store.put(session, entry("wipe.k", "z"));
        store.deleteSession(session);
        expect(store.getAll(session).isEmpty(), "deleteSession 后 getAll 应为空");
        store.deleteSession(session); // 幂等：二次不抛
    }

    private static void scanByPrefixFilters(SessionStateStore store, String session) {
        store.put(session, entry("outbox.1", "o"));
        store.put(session, entry("due.1", "d"));
        store.put(session, entry("other", "x"));
        var scanned = store.scanByPrefix(session, "outbox.");
        expect(scanned.containsKey("outbox.1"), "前缀扫描应含 outbox.1");
        expect(!scanned.containsKey("due.1") && !scanned.containsKey("other"),
                "前缀扫描不应含非前缀键（实际 " + scanned.keySet() + "）");
    }
}
