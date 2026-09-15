package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.util.List;

/**
 * 优雅停机排空预测（spec 1827 / T2855 / impl 1428）——k8s drain /
 * Envoy shutdown drain 思想：停机超时不是拍脑袋常数，而是排空负载的
 * **makespan 预测**——max(最长单会话剩余, 总剩余 ÷ 并行度) × 单位耗时。
 * 两个约束谁主导直接读出：workBound（一个胖子拖死）该先催它、
 * parallelismBound（总量压垮并行）该加并行或延超时。
 *
 * <p>纯函数零状态、只预测不排程（排空执行归宿主）。
 */
public final class DrainForecast {

    private DrainForecast() {
    }

    /** 单会话剩余工作契约：id 非空白、remaining ≥ 0。 */
    public record SessionWork(String sessionId, long remainingUnits) {

        public SessionWork {
            if (sessionId == null || sessionId.isBlank() || remainingUnits < 0) {
                throw new IllegalArgumentException(String.format(
                        "非法剩余工作：id=%s, remaining=%d（要求 id 非空白且 ≥ 0）",
                        sessionId, remainingUnits));
            }
        }
    }

    /**
     * @param sessions           排空会话数
     * @param totalRemainingUnits 剩余工作总量
     * @param makespanMillis     预测排空总时长（两约束取大后换算毫秒）
     * @param bottleneckSession  最长剩余会话（workBound 主导者；空排空 null）
     * @param parallelismBound   true=并行度主导（总量压垮），false=单会话主导
     */
    public record Forecast(int sessions, long totalRemainingUnits, long makespanMillis,
                           String bottleneckSession, boolean parallelismBound) {
    }

    /**
     * 预测入口。契约：parallelism ≥ 1、millisPerUnit ≥ 0（fail-fast）；
     * null 按空表；语义：makespan 单位 = max(最大单会话剩余,
     * ceil(总剩余 ÷ parallelism))，并行度主导判定以 ceil 商严格大于最大
     * 单会话为准（相等即单会话主导——并列取更可操作的处方）。
     */
    public static Forecast forecast(int parallelism, long millisPerUnit,
                                    List<SessionWork> work) {
        if (parallelism < 1) {
            throw new IllegalArgumentException("parallelism 不能小于 1：" + parallelism);
        }
        if (millisPerUnit < 0) {
            throw new IllegalArgumentException("millisPerUnit 不能为负：" + millisPerUnit);
        }
        List<SessionWork> window = work == null ? List.of() : work;
        long total = 0;
        long maxRemaining = 0;
        String bottleneck = null;
        for (SessionWork s : window) {
            total += s.remainingUnits();
            if (s.remainingUnits() > maxRemaining) {
                maxRemaining = s.remainingUnits();
                bottleneck = s.sessionId();
            }
        }
        long parallelSpan = (total + parallelism - 1) / parallelism;
        boolean parallelismBound = parallelSpan > maxRemaining;
        long makespanUnits = Math.max(maxRemaining, parallelSpan);
        return new Forecast(window.size(), total, makespanUnits * millisPerUnit,
                bottleneck, parallelismBound);
    }
}
