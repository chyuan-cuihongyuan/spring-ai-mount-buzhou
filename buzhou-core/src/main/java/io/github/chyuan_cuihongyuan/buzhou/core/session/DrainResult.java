package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.time.Duration;

/**
 * drain（优雅停机）编排结果（spec「06 优雅停机」）。
 *
 * @param drainedCount     等完当前轮次后正常 close 的会话数
 * @param forceKilledCount 超时强杀（取消传播）后 close 的会话数
 * @param totalDuration    drain 总耗时（从开始到全部会话 close 完成）
 */
public record DrainResult(int drainedCount, int forceKilledCount, Duration totalDuration) {

    /** 参与 drain 的会话总数。 */
    public int totalCount() {
        return drainedCount + forceKilledCount;
    }
}
