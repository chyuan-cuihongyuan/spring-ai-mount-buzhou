package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * impl-675 / spec 922：SessionLeaseStore 契约校验套件（spec 705/743 同构扩散——
 * Pact consumer contract testing；租约七方法语义固化为可复用校验，第三方 store
 * 实现自证「多实例安全」语义）。
 *
 * <p>范式同 {@link SessionStateStoreContract}：静态 verify 主源码零 JUnit 依赖、
 * 逐项独立收集不抛（全量跑完给完整清单——首个失败即断会掩盖后续同类错误）。
 *
 * <p>九项语义：acquire 幂等/互斥、renew 持有人限定、release 后可重取（新 fence）、
 * steal fence 严格递增、inspect 状态可见、deleteSession 幂等。
 */
public final class SessionLeaseStoreContract {

    /** 契约 TTL（秒级——测试快）。 */
    private static final Duration CONTRACT_TTL = Duration.ofSeconds(30);

    /** 单项检查结果。 */
    public record CheckResult(String name, boolean passed, String detail) {
    }

    /** 契约报告（全部检查跑完后聚合——不短路）。 */
    public record ContractReport(int total, int passed, List<CheckResult> checks) {

        /** 全过才 true（任一失败 = 实现违反租约语义）。 */
        public boolean allPassed() {
            return passed == total;
        }
    }

    private SessionLeaseStoreContract() {
    }

    /** 跑全部九项契约检查（store 非空 fail-fast）。 */
    public static ContractReport verify(SessionLeaseStore store) {
        if (store == null) {
            throw new IllegalArgumentException("store 必须非空");
        }
        List<CheckResult> checks = new ArrayList<>();
        checks.add(check("acquire-on-free-grants-positive-fence",
                acquireOnFreeGrantsPositiveFence(store)));
        checks.add(check("reacquire-same-owner-idempotent",
                reacquireSameOwnerIdempotent(store)));
        checks.add(check("acquire-other-owner-rejected",
                acquireOtherOwnerRejected(store)));
        checks.add(check("renew-by-holder-succeeds",
                renewByHolderSucceeds(store)));
        checks.add(check("renew-by-non-holder-fails",
                renewByNonHolderFails(store)));
        checks.add(check("release-then-reacquire-new-fence",
                releaseThenReacquireNewFence(store)));
        checks.add(check("steal-increments-fence",
                stealIncrementsFence(store)));
        checks.add(check("inspect-reflects-lease",
                inspectReflectsLease(store)));
        checks.add(check("delete-session-idempotent",
                deleteSessionIdempotent(store)));
        long passed = checks.stream().filter(CheckResult::passed).count();
        return new ContractReport(checks.size(), (int) passed, List.copyOf(checks));
    }

    private static CheckResult check(String name, boolean passed) {
        return new CheckResult(name, passed, passed ? "" : "语义不符（见 spec 922 九项定义）");
    }

    /** ① 空闲会话 acquire 成功且 fence 为正。 */
    private static boolean acquireOnFreeGrantsPositiveFence(SessionLeaseStore store) {
        String s = session("free");
        return store.tryAcquire(s, "owner-a", CONTRACT_TTL).acquired()
                && store.tryAcquire(s, "owner-a", CONTRACT_TTL).fencingToken() > 0;
    }

    /** ② 同 owner 重复 acquire 幂等（同 token）。 */
    private static boolean reacquireSameOwnerIdempotent(SessionLeaseStore store) {
        String s = session("reacquire");
        long first = store.tryAcquire(s, "owner-a", CONTRACT_TTL).fencingToken();
        long second = store.tryAcquire(s, "owner-a", CONTRACT_TTL).fencingToken();
        return first == second;
    }

    /** ③ 持有期内异 owner acquire 被拒。 */
    private static boolean acquireOtherOwnerRejected(SessionLeaseStore store) {
        String s = session("exclusive");
        store.tryAcquire(s, "owner-a", CONTRACT_TTL);
        return !store.tryAcquire(s, "owner-b", CONTRACT_TTL).acquired();
    }

    /** ④ 持有人 renew 成功。 */
    private static boolean renewByHolderSucceeds(SessionLeaseStore store) {
        String s = session("renew-holder");
        LeaseAcquireResult acquire = store.tryAcquire(s, "owner-a", CONTRACT_TTL);
        return store.renew(s, "owner-a", acquire.fencingToken(), CONTRACT_TTL);
    }

    /** ⑤ 非持有人 renew 失败。 */
    private static boolean renewByNonHolderFails(SessionLeaseStore store) {
        String s = session("renew-other");
        store.tryAcquire(s, "owner-a", CONTRACT_TTL);
        return !store.renew(s, "owner-b", 999L, CONTRACT_TTL);
    }

    /** ⑥ release 后同会话可再 acquire（新 fence）。 */
    private static boolean releaseThenReacquireNewFence(SessionLeaseStore store) {
        String s = session("release");
        long first = store.tryAcquire(s, "owner-a", CONTRACT_TTL).fencingToken();
        store.release(s, "owner-a", first);
        LeaseAcquireResult reacquired = store.tryAcquire(s, "owner-a", CONTRACT_TTL);
        return reacquired.acquired() && reacquired.fencingToken() != first;
    }

    /** ⑦ steal 抢占成功且 fence 严格递增。 */
    private static boolean stealIncrementsFence(SessionLeaseStore store) {
        String s = session("steal");
        long original = store.tryAcquire(s, "owner-a", CONTRACT_TTL).fencingToken();
        LeaseAcquireResult stolen = store.steal(s, "owner-b", CONTRACT_TTL);
        return stolen.acquired() && stolen.fencingToken() > original;
    }

    /** ⑧ inspect 反映租约状态（持有人/存在性）。 */
    private static boolean inspectReflectsLease(SessionLeaseStore store) {
        String s = session("inspect");
        store.tryAcquire(s, "owner-a", CONTRACT_TTL);
        return store.inspect(s)
                .map(info -> "owner-a".equals(info.ownerId())).orElse(false);
    }

    /** ⑨ deleteSession 幂等（存在时清除、不存在时无操作不抛）。 */
    private static boolean deleteSessionIdempotent(SessionLeaseStore store) {
        String s = session("delete");
        store.tryAcquire(s, "owner-a", CONTRACT_TTL);
        store.deleteSession(s);
        boolean firstClear = store.inspect(s).isEmpty();
        store.deleteSession(s); // 幂等：第二次无操作不抛
        return firstClear && store.inspect(s).isEmpty();
    }

    private static int counter = 0;

    /** 每项检查独立会话（互不串账——契约套件每检查独立会话先例，spec 744 教训）。 */
    private static String session(String tag) {
        return "contract-" + tag + "-" + ++counter;
    }
}
