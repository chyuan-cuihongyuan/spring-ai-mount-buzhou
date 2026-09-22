package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.List;

/**
 * 阶梯加压计划（spec 1892 / T2985 / impl 1493）——k6/Gatling
 * ramping stages 语义：压力按台阶线性爬坡（本台阶目标 + 台阶时长），
 * 台阶内从上一目标线性插值、全部走完保持末目标。压测负载曲线
 * 「先声明后执行」——采样点与负载档位一一对应。
 *
 * <p>纯函数零状态、确定性。
 */
public final class RampProfile {

    private RampProfile() {
    }

    /** 台阶：本台阶末的目标并发 + 台阶时长（毫秒）。 */
    public record Stage(int target, long durationMillis) {
    }

    /**
     * 时刻 t 的目标并发：首台阶从 0 起线性爬向 target；后续台阶从
     * 上一目标线性爬向本目标；全部走完保持末目标。契约：stages
     * 非空、时长 ≥ 1、目标 ≥ 0、elapsed ≥ 0（fail-fast）。
     */
    public static double targetAt(List<Stage> stages, long elapsedMillis) {
        validate(stages);
        if (elapsedMillis < 0) {
            throw new IllegalArgumentException("elapsed 不能为负：" + elapsedMillis);
        }
        double from = 0;
        long cursor = 0;
        for (Stage stage : stages) {
            long stageEnd = cursor + stage.durationMillis();
            if (elapsedMillis <= stageEnd) {
                long into = Math.max(0, elapsedMillis - cursor);
                return from + (stage.target() - from) * into
                        / (double) stage.durationMillis();
            }
            from = stage.target();
            cursor = stageEnd;
        }
        return from; // 全部走完：保持末目标
    }

    /** 总时长：各台阶时长之和。 */
    public static long totalDuration(List<Stage> stages) {
        validate(stages);
        return stages.stream().mapToLong(Stage::durationMillis).sum();
    }

    /** 峰值目标：各台阶目标最大值。 */
    public static int peakTarget(List<Stage> stages) {
        validate(stages);
        return stages.stream().mapToInt(Stage::target).max().orElse(0);
    }

    private static void validate(List<Stage> stages) {
        if (stages == null || stages.isEmpty()) {
            throw new IllegalArgumentException("台阶表不能为空");
        }
        for (Stage stage : stages) {
            if (stage.durationMillis() < 1) {
                throw new IllegalArgumentException(
                        "台阶时长不能小于 1：" + stage.durationMillis());
            }
            if (stage.target() < 0) {
                throw new IllegalArgumentException(
                        "台阶目标不能为负：" + stage.target());
            }
        }
    }
}
