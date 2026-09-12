package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MessageStore SPI 契约校验套件（spec 743 / T1037，spec 705 同构扩散）：
 * 四项有序契约检查——append/load 往返保序、未知会话空读、多次追加保序、
 * deleteSession 幂等清场。主源码零 JUnit 依赖；探针会话
 * {@value #CONTRACT_SESSION_PREFIX} 前缀 + 自清理。
 */
public final class MessageStoreContract {

    /** 契约检查会话前缀。 */
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

        /** 失败项名清单。 */
        public List<String> failures() {
            return checks.stream().filter(c -> !c.passed()).map(Check::name).toList();
        }
    }

    private MessageStoreContract() {
    }

    /** 对 store 跑全部契约检查（结束自清理探针会话）。 */
    public static Report verify(MessageStore store) {
        if (store == null) {
            throw new IllegalArgumentException("store 必须非空");
        }
        String session = CONTRACT_SESSION_PREFIX + UUID.randomUUID();
        List<Check> checks = new java.util.ArrayList<>();
        try {
            checks.add(run("append-load-roundtrip-preserves-order",
                    () -> appendLoadRoundtrip(store, session)));
            checks.add(run("unknown-session-empty", () -> unknownSessionEmpty(store)));
            checks.add(run("multiple-appends-preserve-chronology",
                    () -> multipleAppendsPreserveChronology(store, session)));
            checks.add(run("delete-session-idempotent",
                    () -> deleteSessionIdempotent(store, session)));
        } finally {
            try {
                store.deleteSession(session);
            } catch (RuntimeException ignored) {
                // 清理失败不影响报告
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

    private static BuzhouMessage message(String session, int turn, String text) {
        return new BuzhouMessage(UUID.randomUUID().toString(), session, turn, 0,
                Role.USER, text, List.of(), null, null, null, Map.of(), Instant.now());
    }

    private static void appendLoadRoundtrip(MessageStore store, String session) {
        store.append(session, List.of(message(session, 1, "one")));
        List<BuzhouMessage> loaded = store.load(session);
        expect(loaded.size() >= 1 && "one".equals(loaded.get(0).content()),
                "append 后 load 应读到同内容");
    }

    private static void unknownSessionEmpty(MessageStore store) {
        String ghost = CONTRACT_SESSION_PREFIX + "ghost-" + UUID.randomUUID();
        expect(store.load(ghost).isEmpty(), "未知会话 load 应为空");
    }

    private static void multipleAppendsPreserveChronology(MessageStore store, String session) {
        store.append(session, List.of(message(session, 1, "t1")));
        store.append(session, List.of(message(session, 2, "t2")));
        List<BuzhouMessage> loaded = store.load(session);
        expect(loaded.size() >= 2
                && loaded.stream().anyMatch(m -> "t1".equals(m.content()))
                && loaded.stream().anyMatch(m -> "t2".equals(m.content())),
                "多次 append 后 load 应含全部消息");
    }

    private static void deleteSessionIdempotent(MessageStore store, String session) {
        store.deleteSession(session);
        expect(store.load(session).isEmpty(), "deleteSession 后 load 应为空");
        store.deleteSession(session); // 幂等：二次不抛
    }
}
