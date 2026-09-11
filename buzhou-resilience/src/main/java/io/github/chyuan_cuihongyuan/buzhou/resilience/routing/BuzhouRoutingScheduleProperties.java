package io.github.chyuan_cuihongyuan.buzhou.resilience.routing;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 时段路由窗口 yml 面（spec 503 / T757，K8s CronJob / Argo Rollouts
 * schedule 思想）：{@code buzhou.routing.schedule.{windows[], check-interval}}。
 * RoutingWindow 同日窗 start<end 严格（跨午夜不预设——两窗拼）、weights
 * 非空（整表替换语义——窗口 weights 即全量活跃权重）；"25:00" 绑定失败
 * 启动红 fail-fast。checkInterval 默认 30s、下限 5s。
 */
@ConfigurationProperties(prefix = "buzhou.routing.schedule")
public record BuzhouRoutingScheduleProperties(List<RoutingWindow> windows,
        Duration checkInterval) {

    /** 默认轮询间隔。 */
    static final Duration DEFAULT_CHECK_INTERVAL = Duration.ofSeconds(30);
    /** 轮询间隔下限。 */
    static final Duration MIN_CHECK_INTERVAL = Duration.ofSeconds(5);

    /** 单个时间窗（同日 start<end 严格；weights 非空整表替换）。 */
    public record RoutingWindow(LocalTime start, LocalTime end, Map<String, Integer> weights) {
        public RoutingWindow {
            if (start == null || end == null) {
                throw new IllegalArgumentException("routing.schedule 窗口 start/end 必填（HH:mm）");
            }
            if (!start.isBefore(end)) {
                throw new IllegalArgumentException(
                        "routing.schedule 窗口须同日 start<end（当前 " + start + "≥" + end
                                + "；跨午夜请用两窗拼）");
            }
            weights = weights == null || weights.isEmpty()
                    ? Map.of() : Map.copyOf(weights);
            if (weights.isEmpty()) {
                throw new IllegalArgumentException("routing.schedule 窗口 weights 非空（整表替换语义）");
            }
        }

        /** 时刻是否在窗内（start≤t<end）。 */
        public boolean contains(LocalTime time) {
            return !time.isBefore(start) && time.isBefore(end);
        }
    }

    public BuzhouRoutingScheduleProperties {
        windows = windows == null ? List.of() : List.copyOf(windows);
        checkInterval = checkInterval == null ? DEFAULT_CHECK_INTERVAL : checkInterval;
        if (checkInterval.toMillis() < MIN_CHECK_INTERVAL.toMillis()) {
            throw new IllegalArgumentException("routing.schedule.check-interval 下限 "
                    + MIN_CHECK_INTERVAL.toSeconds() + "s（当前 " + checkInterval + "）");
        }
    }
}
