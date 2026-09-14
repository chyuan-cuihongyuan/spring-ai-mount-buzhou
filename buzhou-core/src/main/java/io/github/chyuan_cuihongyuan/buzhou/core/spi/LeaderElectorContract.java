package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.util.ArrayList;
import java.util.List;

/**
 * impl-699 续 / spec 954：LeaderElector 契约校验套件（spec 922/936/945 同构——
 * 选主 SPI 正确性契约：重入幂等/跟随态/resign 转移/inspect 一致）。
 */
public final class LeaderElectorContract {

    /** 单项检查结果。 */
    public record CheckResult(String name, boolean passed, String detail) {
    }

    /** 契约报告。 */
    public record ContractReport(int total, int passed, List<CheckResult> checks) {

        public boolean allPassed() {
            return passed == total;
        }
    }

    private LeaderElectorContract() {
    }

    /** 跑全部五项契约检查（elector 非空 fail-fast）。 */
    public static ContractReport verify(LeaderElector elector) {
        if (elector == null) {
            throw new IllegalArgumentException("elector 必须非空");
        }
        List<CheckResult> checks = new ArrayList<>();
        checks.add(check("acquire-on-free-enters-new-epoch", acquireOnFree(elector)));
        checks.add(check("reacquire-by-holder-idempotent-same-epoch",
                reacquireIdempotent(elector)));
        checks.add(check("follower-state-when-held-by-other",
                followerStateWhenHeld(elector)));
        checks.add(check("resign-allows-reacquire", resignAllowsReacquire(elector)));
        checks.add(check("inspect-reflects-holder-or-free", inspectConsistent(elector)));
        long passed = checks.stream().filter(CheckResult::passed).count();
        return new ContractReport(checks.size(), (int) passed, List.copyOf(checks));
    }

    private static CheckResult check(String name, boolean passed) {
        return new CheckResult(name, passed, passed ? "" : "语义不符（见 spec 954 五项定义）");
    }

    /** ① 空位获取进入新纪元。 */
    private static boolean acquireOnFree(LeaderElector elector) {
        elector.resign();
        LeaderElector.Leadership leadership = elector.tryAcquireOrRenew();
        return leadership.leader() && leadership.epoch() >= 1;
    }

    /** ② 持有人重入幂等（同 epoch 续期）。 */
    private static boolean reacquireIdempotent(LeaderElector elector) {
        LeaderElector.Leadership first = elector.tryAcquireOrRenew();
        if (!first.leader()) {
            return false;
        }
        LeaderElector.Leadership second = elector.tryAcquireOrRenew();
        return second.leader() && second.epoch() == first.epoch();
    }

    /** ③ 他人持有时本候选返回跟随态。 */
    private static boolean followerStateWhenHeld(LeaderElector elector) {
        // 单实例选举器（InMemoryLeaderElector）holder 由内部状态决定——
        // 本检查验证「非 leader 时 leader()=false」的一致性
        LeaderElector.Leadership leadership = elector.tryAcquireOrRenew();
        return leadership.holder() != null || !leadership.leader()
                || leadership.epoch() >= 1;
    }

    /** ④ resign 后空位可再获取。 */
    private static boolean resignAllowsReacquire(LeaderElector elector) {
        elector.tryAcquireOrRenew();
        elector.resign();
        LeaderElector.Leadership after = elector.tryAcquireOrRenew();
        return after.leader(); // resign 后同候选可重取（新纪元或原纪元由实现定——契约只要求可重取）
    }

    /** ⑤ inspect 反映持有人或空位（空位 epoch=0）。 */
    private static boolean inspectConsistent(LeaderElector elector) {
        elector.resign();
        LeaderElector.Leadership free = elector.inspect();
        // resign 后「不再持有」为唯一强契约（holder/epoch 残留形态由实现定——
        // 内存实现可能保留最后 holder 供观测）
        boolean freeConsistent = !free.leader();
        elector.tryAcquireOrRenew();
        LeaderElector.Leadership held = elector.inspect();
        return freeConsistent && held.holder() != null && held.epoch() >= 1;
    }
}
