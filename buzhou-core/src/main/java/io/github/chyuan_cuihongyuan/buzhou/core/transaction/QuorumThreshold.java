package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

/**
 * 票数下限判定（spec 1904 / T3009 / impl 1505）——分布式共识两档
 * 容错的票数公式：崩溃容错（Paxos/Raft）多数派 = ⌊N/2⌋+1（N ≥
 * 2f+1）；拜占庭容错 = N ≥ 3f+1（⌊(N−1)/3⌋ 个坏票可容）。两档混淆
 * 的配置错误（2f+1 想容拜占庭）一行公式拦住。
 *
 * <p>纯函数零状态；整数除法向下取整（诚实下限）。
 */
public final class QuorumThreshold {

    private QuorumThreshold() {
    }

    /**
     * 简单多数票数下限：⌊N/2⌋+1——崩溃容错语义（N ≥ 2f+1 时可容
     * f 个崩溃故障）。契约：voters ≥ 1（fail-fast）。
     */
    public static int majority(int voters) {
        if (voters < 1) {
            throw new IllegalArgumentException("voters 不能小于 1：" + voters);
        }
        return voters / 2 + 1;
    }

    /**
     * 拜占庭坏票容忍数：⌊(N−1)/3⌋——该投票者规模下可容忍的最大
     * 拜占庭（任意行为）票数。契约：voters ≥ 1（fail-fast）。
     */
    public static int byzantineTolerance(int voters) {
        if (voters < 1) {
            throw new IllegalArgumentException("voters 不能小于 1：" + voters);
        }
        return (voters - 1) / 3;
    }

    /**
     * 容忍 f 个拜占庭坏票所需的最小投票者数：3f+1。契约：faults ≥ 0
     * （fail-fast）。
     */
    public static int byzantineSize(int faults) {
        if (faults < 0) {
            throw new IllegalArgumentException("faults 不能为负：" + faults);
        }
        return 3 * faults + 1;
    }
}
