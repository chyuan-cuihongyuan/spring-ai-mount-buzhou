package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import java.util.ArrayList;
import java.util.List;

/**
 * 负载脱落阶梯（spec 1816 / T2833 / impl 1417）——Envoy overload manager /
 * Akka 断路器组思想：过载处置不是一刀切全拒，而是**按优先级阶梯逐级甩**——
 * 低优先级工作先掉（阈值低），负载继续涨才轮到高优先级（阈值高）。每级
 * 一个脱落阈值，负载因子越过哪级阈值哪级开始拒新——「过载时谁先死」是
 * 显式声明而非运气。
 *
 * <p>纯函数零状态、只裁决不执行（脱落动作归宿主）；负载因子口径（并发/
 * 利用率/队列深）由调用方声明。
 */
public final class LoadShedLadder {

    private LoadShedLadder() {
    }

    /** 单阶梯级契约：name 非空白、shedThreshold ≥ 0（低阈值=低优先级先掉）。 */
    public record Level(String name, double shedThreshold) {

        public Level {
            if (name == null || name.isBlank() || Double.isNaN(shedThreshold)
                    || shedThreshold < 0) {
                throw new IllegalArgumentException(
                        "非法阶梯级：name=" + name + ", threshold=" + shedThreshold
                                + "（要求 name 非空白且 threshold ≥ 0 非 NaN）");
            }
        }
    }

    /**
     * @param loadFactor 当前负载因子（口径自声明，如利用率 0.0–1.0 或并发数）
     * @param shedLevels 已越过阈值的级（入参序，即开始拒新的级）
     * @param keptLevels 仍在服务的级
     */
    public record ShedDecision(double loadFactor, List<String> shedLevels,
                               List<String> keptLevels) {

        /** 脱落面占比 = shed/(全部)（无级 -1 哨兵）。 */
        public double shedRatio() {
            int total = shedLevels.size() + keptLevels.size();
            return total == 0 ? -1d : (double) shedLevels.size() / total;
        }

        /** 是否已开始脱落（最低一级已掉——升级信号）。 */
        public boolean escalating() {
            return !shedLevels.isEmpty();
        }
    }

    /**
     * 阶梯裁决入口。契约：loadFactor ≥ 0 非 NaN（fail-fast）；null 按空表。
     * 语义：loadFactor ≥ 级阈值即该级脱落（含边界）。
     */
    public static ShedDecision decide(double loadFactor, List<Level> ladder) {
        if (Double.isNaN(loadFactor) || loadFactor < 0) {
            throw new IllegalArgumentException(
                    "loadFactor 须 ≥ 0 且非 NaN：" + loadFactor);
        }
        List<Level> rungs = ladder == null ? List.of() : ladder;
        List<String> shed = new ArrayList<>();
        List<String> kept = new ArrayList<>();
        for (Level level : rungs) {
            if (loadFactor >= level.shedThreshold()) {
                shed.add(level.name());
            } else {
                kept.add(level.name());
            }
        }
        return new ShedDecision(loadFactor, List.copyOf(shed), List.copyOf(kept));
    }
}
