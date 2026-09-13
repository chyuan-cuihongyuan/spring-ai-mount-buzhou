package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import java.time.Instant;
import java.util.List;

/**
 * impl-693 / spec 944：工具调用结局分布统计（spec 50 事件溯源工具日志的聚合
 * 读面）——TIMEOUT 高占比 = 超时配置问题；CANCELLED 高占比 = 取消风暴；分布
 * 是根因分诊第一层。
 *
 * <p>纯函数零 IO（单 null outcome 条目跳过——诚实计数口径）。恢复域已有
 * {@link RecoverySupport}（重放执行面）；本类是只读统计面。
 */
public final class ToolCallOutcomeStats {

    /**
     * 结局分布（四主桶 + other 收容桶）——total() == 四桶+other 恒成立（枚举
     * 未来扩展自动落入 other，守恒不破）。
     */
    public record OutcomeStats(long completed, long failed, long timeouts, long cancelled,
                               long other) {
        /** 总条数（守恒：四桶 + other）。 */
        public long total() {
            return completed + failed + timeouts + cancelled + other;
        }
    }

    private ToolCallOutcomeStats() {
    }

    /** 统计（entries null fail-fast；单条 null outcome 跳过）。 */
    public static OutcomeStats stats(List<ToolCallLogEntry> entries) {
        if (entries == null) {
            throw new IllegalArgumentException("entries 必须非空");
        }
        long completed = 0;
        long failed = 0;
        long timeouts = 0;
        long cancelled = 0;
        long other = 0;
        for (ToolCallLogEntry entry : entries) {
            if (entry == null || entry.outcome() == null) {
                continue;
            }
            switch (entry.outcome()) {
                case COMPLETED -> completed++;
                case FAILED -> failed++;
                case TIMEOUT -> timeouts++;
                case CANCELLED -> cancelled++;
                default -> other++; // 枚举扩展自动收容——守恒不破
            }
        }
        return new OutcomeStats(completed, failed, timeouts, cancelled, other);
    }
}
