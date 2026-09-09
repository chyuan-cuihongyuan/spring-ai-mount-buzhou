package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * 工具泳道优先级 yml 面（spec 422 / T736，Envoy priority levels 接线）：
 * {@code buzhou.tool-lanes.lanes.<泳道名>.{permits, acquire-timeout}} +
 * {@code buzhou.tool-lanes.tools.<工具名>.{lane, priority}}。声明即装配
 * （wrapToolCallbacks 名匹配 → PriorityLaneToolCallback 共享命名单例泳道）；
 * 空表/未配置零行为变化；tools 引用未声明泳道启动即红（fail-fast）。
 */
@ConfigurationProperties(prefix = "buzhou.tool-lanes")
public record BuzhouToolLaneProperties(Map<String, LaneSpec> lanes, Map<String, ToolBinding> tools) {

    /** 中位默认优先级（0-9 有界——未声明显式 priority 的工具落此）。 */
    public static final int DEFAULT_PRIORITY = 5;
    /** 默认许可等待超时（排队优先于拒绝——与 LaneLimitingToolCallback 语义对齐）。 */
    public static final Duration DEFAULT_ACQUIRE_TIMEOUT = Duration.ofSeconds(30);

    public BuzhouToolLaneProperties {
        lanes = lanes == null ? Map.of() : Map.copyOf(lanes);
        tools = tools == null ? Map.of() : Map.copyOf(tools);
    }

    /** 泳道容量声明（单规范构造器——缺省 permits=1、acquire-timeout=30s）。 */
    public record LaneSpec(Integer permits, Duration acquireTimeout) {
        public LaneSpec {
            permits = permits == null ? 1 : permits;
            acquireTimeout = acquireTimeout == null ? DEFAULT_ACQUIRE_TIMEOUT : acquireTimeout;
            if (permits < 1) {
                throw new IllegalArgumentException("泳道 permits>=1（当前 " + permits + "）");
            }
            if (acquireTimeout.isZero() || acquireTimeout.isNegative()) {
                throw new IllegalArgumentException("acquire-timeout 为正（当前 " + acquireTimeout + "）");
            }
        }
    }

    /** 工具绑定（缺省 priority=5 中位；lane 必须在 lanes 声明内——装配期 fail-fast）。 */
    public record ToolBinding(String lane, Integer priority) {
        public ToolBinding {
            priority = priority == null ? DEFAULT_PRIORITY : priority;
            if (lane == null || lane.isBlank()) {
                throw new IllegalArgumentException("工具绑定的 lane 非空");
            }
        }
    }
}
