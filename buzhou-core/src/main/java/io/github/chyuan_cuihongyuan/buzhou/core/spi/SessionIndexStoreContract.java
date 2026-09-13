package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * impl-694 / spec 945：SessionIndexStore 契约校验套件（spec 922/936 同构）。
 * 五项语义：upsert→get 往返一致、覆盖幂等、delete 幂等、DELETED 排除、
 * purgeOlderThan 计数/limit 尊重。
 */
public final class SessionIndexStoreContract {

    /** 单项检查结果。 */
    public record CheckResult(String name, boolean passed, String detail) {
    }

    /** 契约报告。 */
    public record ContractReport(int total, int passed, List<CheckResult> checks) {

        public boolean allPassed() {
            return passed == total;
        }
    }

    private SessionIndexStoreContract() {
    }

    /** 跑全部五项契约检查（store 非空 fail-fast）。 */
    public static ContractReport verify(SessionIndexStore store) {
        if (store == null) {
            throw new IllegalArgumentException("store 必须非空");
        }
        List<CheckResult> checks = new ArrayList<>();
        checks.add(check("upsert-get-roundtrip", upsertGetRoundtrip(store)));
        checks.add(check("upsert-overwrite-idempotent", upsertOverwriteIdempotent(store)));
        checks.add(check("delete-idempotent", deleteIdempotent(store)));
        checks.add(check("list-excludes-deleted", listExcludesDeleted(store)));
        checks.add(check("purge-returns-count-and-respects-limit",
                purgeReturnsCountAndRespectsLimit(store)));
        long passed = checks.stream().filter(CheckResult::passed).count();
        return new ContractReport(checks.size(), (int) passed, List.copyOf(checks));
    }

    private static CheckResult check(String name, boolean passed) {
        return new CheckResult(name, passed, passed ? "" : "语义不符（见 spec 945 五项定义）");
    }

    private static SessionInfo info(String sessionId) {
        return new SessionInfo(sessionId, "app", "agent",
                SessionInfo.STATUS_ACTIVE, Instant.EPOCH.toEpochMilli(),
                Instant.EPOCH.toEpochMilli(), 1, java.util.Map.of());
    }

    /** ① upsert→get 往返一致。 */
    private static boolean upsertGetRoundtrip(SessionIndexStore store) {
        String s = session("rt");
        store.upsert(info(s));
        return store.get(s).map(i -> i.sessionId().equals(s)).orElse(false);
    }

    /** ② 同 id 重复 upsert 覆盖幂等（后写胜，不产生重复行）。 */
    private static boolean upsertOverwriteIdempotent(SessionIndexStore store) {
        String s = session("ow");
        store.upsert(info(s));
        store.upsert(info(s));
        return store.get(s).isPresent()
                && store.list(SessionIndexQuery.defaults()).stream()
                        .filter(i -> i.sessionId().equals(s)).count() == 1;
    }

    /** ③ delete 后 get empty 且二次 delete 无操作不抛。 */
    private static boolean deleteIdempotent(SessionIndexStore store) {
        String s = session("del");
        store.upsert(info(s));
        store.delete(s);
        boolean emptyAfter = store.get(s).isEmpty();
        store.delete(s);
        return emptyAfter && store.get(s).isEmpty();
    }

    /** ④ list 默认排除 DELETED 状态行（spec 33 §B 审计行显式过滤口径）。 */
    private static boolean listExcludesDeleted(SessionIndexStore store) {
        String s = session("deleted");
        SessionInfo deleted = new SessionInfo(s, "app", "agent",
                SessionInfo.STATUS_DELETED, Instant.EPOCH.toEpochMilli(),
                Instant.EPOCH.toEpochMilli(), 0, java.util.Map.of());
        store.upsert(deleted);
        boolean excluded = store.list(SessionIndexQuery.defaults()).stream()
                .noneMatch(i -> i.sessionId().equals(s));
        store.delete(s);
        return excluded;
    }

    /**
     * ⑤ purgeOlderThan：只清非 ACTIVE（CLOSED/DELETED——活跃会话保护），
     * 按 lastActiveAt 截止删除并返回计数、limit 尊重。
     */
    private static boolean purgeReturnsCountAndRespectsLimit(SessionIndexStore store) {
        String base = session("purge");
        SessionInfo closed1 = new SessionInfo(base + "-c1", "app", "agent",
                SessionInfo.STATUS_CLOSED, 1L, 1L, 0, java.util.Map.of());
        SessionInfo closed2 = new SessionInfo(base + "-c2", "app", "agent",
                SessionInfo.STATUS_CLOSED, 2L, 2L, 0, java.util.Map.of());
        SessionInfo active = new SessionInfo(base + "-act", "app", "agent",
                SessionInfo.STATUS_ACTIVE, 1L, 1L, 0, java.util.Map.of());
        store.upsert(closed1);
        store.upsert(closed2);
        store.upsert(active);
        int purged = store.purgeOlderThan(Instant.ofEpochMilli(3), 1);
        // 遍历序无保证——只断言恰好删 1 个 CLOSED（ACTIVE 保留）
        boolean limitRespected = purged == 1
                && (store.get(base + "-c1").isEmpty() ^ store.get(base + "-c2").isEmpty());
        // limit 放开：剩余 CLOSED 清完，ACTIVE 不被清（保护语义）
        int rest = store.purgeOlderThan(Instant.ofEpochMilli(3), 10);
        return limitRespected && rest == 1 && store.get(base + "-act").isPresent();
    }

    private static int counter = 0;

    private static String session(String tag) {
        return "idx-contract-" + tag + "-" + ++counter;
    }
}
