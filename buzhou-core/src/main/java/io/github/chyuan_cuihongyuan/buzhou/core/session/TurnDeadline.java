package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Turn Deadline 值对象（spec 13 §core-2 / impl-28；源 gRPC-Java {@code Deadline} 语义 +
 * Kafka {@code delivery.timeout} 端到端预算思想）：把「本 Turn 还剩多少时间」对象化为
 * <b>绝对时刻</b>，各等待点（工具派发、外层 join、组锁、并发许可）统一按 {@link #remaining()}
 * 限时等待——<b>剩余时间传递</b>而非每层重新计时，嵌套工具调用不会让总时长超过 Turn 预算。
 *
 * <p>语义要点：
 *
 * <ul>
 *   <li>{@link #none()} 哨兵 = 不设界（保持既有「无限等待」行为，兼容默认配置）；</li>
 *   <li>{@link #remaining()} 负值归零（到期后恒为零，不会出现负等待）；哨兵返回
 *       {@link #UNBOUNDED_REMAINING}（足够大的哨兵剩余量，在 {@code min} 组合中恒退位给任何
 *       有限值）；</li>
 *   <li>{@link #min(TurnDeadline)} 组合器取更早的绝对时刻（嵌套预算收紧只减不增）；</li>
 *   <li>内部计时基于 {@link Instant}（系统时钟）：单进程内 Turn 级预算可接受；NTP 回拨会
 *       等比放宽/收紧剩余量，跨进程时钟同步不依赖本对象。</li>
 * </ul>
 *
 * <p>与 {@link CancellationToken}（CancelMode 三档）<b>并列传播、不合并</b>：取消表达
 * 「外部意愿」，Deadline 表达「时间预算」，两者在派发与 join 处分别生效、互不吞没。
 *
 * @param deadline 截止绝对时刻；{@code null} = {@link #none()} 哨兵（不设界）
 */
public record TurnDeadline(Instant deadline) {

    /**
     * 不设界时的哨兵剩余量：约 2.9×10^11 年，任何有限预算与其取 min 恒等于该有限预算；
     * 以毫秒交给限时等待 API 时饱和为 {@code Long.MAX_VALUE}，等价于无限等待。
     */
    public static final Duration UNBOUNDED_REMAINING = Duration.ofSeconds(Long.MAX_VALUE);

    /** 不设 Deadline 的哨兵（无界等待，既有默认行为）。 */
    public static TurnDeadline none() {
        return new TurnDeadline(null);
    }

    /** 以绝对时刻构造（{@code deadline} 不允许 null——不设界请用 {@link #none()}）。 */
    public static TurnDeadline at(Instant deadline) {
        return new TurnDeadline(Objects.requireNonNull(deadline, "deadline"));
    }

    /** 从当前时刻起的预算构造。 */
    public static TurnDeadline in(Duration budget) {
        Objects.requireNonNull(budget, "budget");
        return at(Instant.now().plus(budget));
    }

    /** 是否为不设界哨兵。 */
    public boolean isNone() {
        return deadline == null;
    }

    /**
     * 剩余时间：以当前时刻计算，负值归零（到期恒为零）；哨兵返回
     * {@link #UNBOUNDED_REMAINING}（见类注释）。等待方应先判 {@link #isNone()} 决定
     * 「无限等」还是「按剩余等」，哨兵剩余量仅用于组合计算不越界。
     */
    public Duration remaining() {
        if (deadline == null) {
            return UNBOUNDED_REMAINING;
        }
        Duration remaining = Duration.between(Instant.now(), deadline);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    /** 剩余毫秒数（{@link #remaining()} 的便捷形式，等待 API 直接可用）。 */
    public long remainingMillis() {
        if (deadline == null) {
            return Long.MAX_VALUE;
        }
        return remaining().toMillis();
    }

    /** 是否已到期（当前时刻不早于截止时刻；哨兵永不到期）。 */
    public boolean isExpired() {
        return deadline != null && !Instant.now().isBefore(deadline);
    }

    /**
     * 组合器：取两者中更早的截止时刻（更紧的预算）。
     * 哨兵退位——任一为有限 Deadline 即以有限者为准；两者皆哨兵仍为哨兵。
     */
    public TurnDeadline min(TurnDeadline other) {
        if (isNone()) {
            return other == null ? this : other;
        }
        if (other == null || other.isNone()) {
            return this;
        }
        return deadline.isBefore(other.deadline) ? this : other;
    }
}
