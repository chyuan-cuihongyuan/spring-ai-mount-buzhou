package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.ArrayList;
import java.util.List;

/**
 * 扇出 pacing 计划（spec 1809 / T2819 / impl 1410）——TCP 拥塞控制 pacing /
 * 初始拥塞窗口（IW）思想：并行扇出 N 个调用一股脑全发是惊群（下游瞬时
 * 过载、自己排队挨饿）；**头部 K 个立即发**（TCP IW——小扇出根本不用节流），
**其余按间隔匀速放行**（pacing——大扇出摊平到达曲线）。头部免节流 + 尾部
 * pacing 的组合，让小任务流零延迟、大任务流不惊群。
 *
 * <p>纯函数零状态：输入为扇出规模、放行间隔、头部免节流名额；输出逐任务
 * 起发延迟。只排程不执行（派发归宿主）。
 */
public final class FanoutPacingPlan {

    private FanoutPacingPlan() {
    }

    /** 单任务起发：delayMillis=0 即立即发（头部免节流名额内）。 */
    public record TaskStart(int taskIndex, long delayMillis) {
    }

    /**
     * @param fanout        扇出任务总数
     * @param intervalMillis 尾部 pacing 间隔（毫秒）
     * @param headStart     头部免节流名额（IW 语义；0 = 全量 pacing）
     * @param starts        逐任务起发延迟（任务序）
     */
    public record Plan(int fanout, long intervalMillis, int headStart,
                       List<TaskStart> starts) {

        /** 计划总跨度 = 末任务延迟（零扇出/全头部 0）。 */
        public long totalSpanMillis() {
            return starts.isEmpty() ? 0 : starts.get(starts.size() - 1).delayMillis();
        }

        /** 被节流占比 = 超出头部名额的任务占比（零扇出 -1 哨兵）。 */
        public double pacedRatio() {
            return fanout == 0 ? -1d : (double) Math.max(0, fanout - headStart) / fanout;
        }
    }

    /**
     * 排程入口。契约：fanout ≥ 0、intervalMillis ≥ 1、0 ≤ headStart ≤ fanout
     * （fail-fast）；语义：前 headStart 个任务延迟 0；第 i（≥ headStart）个
     * 延迟 (i − headStart + 1) × interval。
     */
    public static Plan plan(int fanout, long intervalMillis, int headStart) {
        if (fanout < 0) {
            throw new IllegalArgumentException("fanout 不能为负：" + fanout);
        }
        if (intervalMillis < 1) {
            throw new IllegalArgumentException("intervalMillis 不能小于 1：" + intervalMillis);
        }
        if (headStart < 0 || headStart > fanout) {
            throw new IllegalArgumentException(
                    "headStart 越界：" + headStart + "（要求 0 ≤ headStart ≤ fanout=" + fanout + "）");
        }
        List<TaskStart> starts = new ArrayList<>(fanout);
        for (int i = 0; i < fanout; i++) {
            long delay = i < headStart ? 0 : (long) (i - headStart + 1) * intervalMillis;
            starts.add(new TaskStart(i, delay));
        }
        return new Plan(fanout, intervalMillis, headStart, List.copyOf(starts));
    }
}
