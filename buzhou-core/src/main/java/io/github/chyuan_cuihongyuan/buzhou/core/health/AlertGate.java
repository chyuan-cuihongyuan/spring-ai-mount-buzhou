package io.github.chyuan_cuihongyuan.buzhou.core.health;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 告警通知策略门（spec 330 / T651，Prometheus Alertmanager silence / inhibit
 * 借鉴）：挂在告警引擎通知路径上——<b>观察先于判定</b>（被吞的是通知，不是
 * 事实：引擎状态机照常推进，firing 视图照常更新）。
 *
 * <ul>
 *   <li><b>静默窗</b>：机制匹配（{@code *} = 全量）+ 到期时刻；窗口内匹配机制
 *       的触发/恢复通知均被吞；过期惰性清理（观察路径顺带，无定时器）；
 *       yml 声明与运行时事故按钮双入口（不重启即时生效）。</li>
 *   <li><b>抑制规则</b>：源机制 firing 时目标机制通知被抑制（根因遮蔽衍生）；
 *       被抑制的 FIRING 仍进 firing 视图——源恢复后目标再触发可再通知。</li>
 *   <li>吞必有痕：WARN + {@code buzhou.alert.silenced} / {@code buzhou.alert.inhibited}
 *       计数器；被吞通知不重放（诚实语义）。</li>
 * </ul>
 */
public final class AlertGate {

    /** 静默窗（mechanisms 含 {@code *} 即全量匹配）。 */
    public record Silence(String id, Set<String> mechanisms, Instant until,
            String comment, String createdBy, Instant createdAt) {

        public Silence {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("id 非空");
            }
            if (mechanisms == null || mechanisms.isEmpty()) {
                throw new IllegalArgumentException("mechanisms 非空（silence=" + id + "）");
            }
            for (String mechanism : mechanisms) {
                if (mechanism == null || mechanism.isBlank()) {
                    throw new IllegalArgumentException("mechanisms 元素非空（silence=" + id + "）");
                }
            }
            if (until == null) {
                throw new IllegalArgumentException("until 非空（silence=" + id + "）");
            }
            mechanisms = Set.copyOf(mechanisms);
            createdAt = createdAt == null ? Instant.EPOCH : createdAt;
        }

        boolean matches(String mechanism) {
            return mechanisms.contains("*") || mechanisms.contains(mechanism);
        }
    }

    /** 抑制规则：source firing 时 target 通知被抑制。 */
    public record InhibitRule(String sourceMechanism, String targetMechanism) {

        public InhibitRule {
            if (sourceMechanism == null || sourceMechanism.isBlank()
                    || targetMechanism == null || targetMechanism.isBlank()) {
                throw new IllegalArgumentException("source/target-mechanism 非空（inhibit=" + this + "）");
            }
            if (sourceMechanism.equals(targetMechanism)) {
                throw new IllegalArgumentException(
                        "source/target 不得同机制（自抑=永久吞通知，inhibit=" + sourceMechanism + "）");
            }
        }
    }

    /** 一枚被吞通知的留痕（静默优先报告；抑制时 silence 为 null）。 */
    public record Silenced(String mechanism, Silence silence, InhibitRule inhibitRule) {
    }

    private static final System.Logger LOG = System.getLogger(AlertGate.class.getName());

    private final List<InhibitRule> inhibitRules;
    private final Map<String, Silence> silences = new ConcurrentHashMap<>();
    private final Set<String> firingMechanisms = ConcurrentHashMap.newKeySet();
    private final Supplier<Instant> clock;
    private final AtomicLong silenceSeq = new AtomicLong();

    /**
     * @param inhibitRules 静态抑制规则（yml 声明）
     * @param initialSilences 初始静默窗（yml 声明；until 已由装配层换算）
     * @param clock          时钟（测试注入伪时钟）
     */
    public AlertGate(List<InhibitRule> inhibitRules, List<Silence> initialSilences,
            Supplier<Instant> clock) {
        this.inhibitRules = inhibitRules == null ? List.of() : List.copyOf(inhibitRules);
        if (initialSilences != null) {
            initialSilences.forEach(s -> silences.put(s.id(), s));
        }
        this.clock = clock == null ? Instant::now : clock;
    }

    /**
     * 观察一枚触发/恢复并判定是否放行通知：先更新 firing 视图，再判静默
     * （优先报告），后判抑制；吞则留痕（WARN + 计数器）并返回 false。
     */
    public boolean observe(AlertRuleEngine.AlertFiring alert) {
        if (alert == null) {
            return true;
        }
        if (alert.recovered()) {
            firingMechanisms.remove(alert.mechanism());
        } else {
            firingMechanisms.add(alert.mechanism());
        }
        Silenced swallowed = decide(alert);
        if (swallowed == null) {
            return true;
        }
        if (swallowed.silence() != null) {
            LOG.log(System.Logger.Level.WARNING,
                    "告警通知被静默吞下：mechanism={0} silence={1}（{2}，by {3}，至 {4}）",
                    alert.mechanism(), swallowed.silence().id(), swallowed.silence().comment(),
                    swallowed.silence().createdBy(), swallowed.silence().until());
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder
                    .metrics().counter("buzhou.alert.silenced", 1,
                            "mechanism", alert.mechanism());
        } else {
            LOG.log(System.Logger.Level.WARNING,
                    "告警通知被抑制吞下：mechanism={0} 根因 {1} 仍在 firing（inhibit {1}->{0}）",
                    alert.mechanism(), swallowed.inhibitRule().sourceMechanism());
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder
                    .metrics().counter("buzhou.alert.inhibited", 1,
                            "mechanism", alert.mechanism());
        }
        return false;
    }

    private Silenced decide(AlertRuleEngine.AlertFiring alert) {
        pruneExpired();
        for (Silence silence : silences.values()) {
            if (silence.matches(alert.mechanism())) {
                return new Silenced(alert.mechanism(), silence, null);
            }
        }
        for (InhibitRule rule : inhibitRules) {
            if (rule.targetMechanism().equals(alert.mechanism())
                    && firingMechanisms.contains(rule.sourceMechanism())) {
                return new Silenced(alert.mechanism(), null, rule);
            }
        }
        return null;
    }

    /** 运行时事故按钮：开一扇静默窗（不重启即时生效）。 */
    public Silence silence(Set<String> mechanisms, Duration duration, String comment,
            String createdBy) {
        if (mechanisms == null || mechanisms.isEmpty()) {
            throw new IllegalArgumentException("mechanisms 非空");
        }
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("duration 为正");
        }
        Instant now = clock.get();
        Silence silence = new Silence("silence-" + silenceSeq.incrementAndGet(),
                mechanisms, now.plus(duration), comment == null ? "" : comment,
                createdBy == null ? "" : createdBy, now);
        silences.put(silence.id(), silence);
        return silence;
    }

    /** 运行时事故按钮：撤销静默窗（不存在/已过期返回 false）。 */
    public boolean cancelSilence(String id) {
        return id != null && silences.remove(id) != null;
    }

    /** 活跃静默窗（过期惰性清理后观察，装配/创建时间序）。 */
    public List<Silence> activeSilences() {
        pruneExpired();
        return silences.values().stream()
                .sorted(java.util.Comparator.comparing(Silence::createdAt)
                        .thenComparing(Silence::id))
                .toList();
    }

    /** firing 视图（观测面——抑制判定的依据）。 */
    public Set<String> firingMechanisms() {
        return Set.copyOf(firingMechanisms);
    }

    /** 启动期校验：静默/抑制引用的机制必须存在（yml 错该红；{@code *} 恒过）。 */
    public void validateMechanisms(Map<String, BuzhouHealth> healths) {
        for (Silence silence : silences.values()) {
            for (String mechanism : silence.mechanisms()) {
                if (!mechanism.equals("*") && !healths.containsKey(mechanism)) {
                    throw new IllegalArgumentException("静默窗「" + silence.id()
                            + "」引用机制 " + mechanism + " 不存在；可用机制：" + healths.keySet());
                }
            }
        }
        for (InhibitRule rule : inhibitRules) {
            for (String mechanism : new String[]{rule.sourceMechanism(), rule.targetMechanism()}) {
                if (!healths.containsKey(mechanism)) {
                    throw new IllegalArgumentException("抑制规则 " + rule.sourceMechanism()
                            + "->" + rule.targetMechanism() + " 引用机制 " + mechanism
                            + " 不存在；可用机制：" + healths.keySet());
                }
            }
        }
    }

    private void pruneExpired() {
        Instant now = clock.get();
        silences.values().removeIf(s -> !now.isBefore(s.until()));
    }

    /** 静态抑制规则（观测面）。 */
    public List<InhibitRule> inhibitRules() {
        return inhibitRules;
    }

    /** 装配助手：yml 声明窗换算 until（now + duration）。 */
    public static Silence ymlSilence(String id, Set<String> mechanisms, Duration duration,
            String comment, String createdBy, Instant now) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("duration 为正（silence=" + id + "）");
        }
        return new Silence(id, mechanisms, now.plus(duration),
                comment == null ? "" : comment, createdBy == null ? "" : createdBy, now);
    }
}
