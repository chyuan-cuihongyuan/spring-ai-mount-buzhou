package io.github.chyuan_cuihongyuan.buzhou.core.health;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 健康告警规则引擎（spec 312 / T615，Grafana ruler 借鉴——规则即声明、
 * 数据源即健康面、通道即回调）：周期评估机制健康，DOWN 持续满 {@code for}
 * 窗 → FIRING；恢复 UP → RECOVERED（双向通知宿主回调 + 计数 + WARN）。
 * flap 被 for 窗吸收；UNKNOWN ≠ DOWN（未启用机制不告警）。
 *
 * <p>spec 330 / T652：通知路径可挂 {@link AlertGate}（静默窗 + 抑制规则）——
 * 未挂时通知行为与 312 完全一致；挂上时被吞的只是通知，状态机照常推进。
 */
public final class AlertRuleEngine implements org.springframework.context.SmartLifecycle {

    /** 告警规则（yml 声明形态）。 */
    public record AlertRule(String name, String mechanism, Duration forDuration) {

        public AlertRule {
            if (name == null || name.isBlank() || mechanism == null || mechanism.isBlank()) {
                throw new IllegalArgumentException("name/mechanism 非空（rule=" + name + "）");
            }
            forDuration = forDuration == null ? Duration.ZERO : forDuration;
            if (forDuration.isNegative()) {
                throw new IllegalArgumentException("for 为非负时长（rule=" + name + "）");
            }
        }
    }

    /** 一次触发/恢复（通知载荷）。 */
    public record AlertFiring(String ruleName, String mechanism, boolean recovered,
                              Instant at, Map<String, Object> details) {
    }

    private final List<AlertRule> rules;
    private final Supplier<Map<String, BuzhouHealth>> healthSource;
    private final Duration interval;
    private final AlertGate gate; // 可选——null = 无策略门（312 原语义）
    private final List<Consumer<AlertFiring>> listeners = new CopyOnWriteArrayList<>();
    private final Map<String, Instant> downSince = new ConcurrentHashMap<>();
    private final Map<String, Boolean> firing = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicBoolean running =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private volatile java.util.concurrent.ScheduledExecutorService scheduler;

    public AlertRuleEngine(List<AlertRule> rules, Supplier<Map<String, BuzhouHealth>> healthSource) {
        this(rules, healthSource, Duration.ofSeconds(30));
    }

    public AlertRuleEngine(List<AlertRule> rules, Supplier<Map<String, BuzhouHealth>> healthSource,
            Duration interval) {
        this(rules, healthSource, interval, null);
    }

    /** spec 330：带通知策略门的构造（gate 为 null 时与上方构造完全一致）。 */
    public AlertRuleEngine(List<AlertRule> rules, Supplier<Map<String, BuzhouHealth>> healthSource,
            Duration interval, AlertGate gate) {
        if (rules == null || rules.isEmpty()) {
            throw new IllegalArgumentException("rules 非空——无规则不建引擎");
        }
        if (interval == null || interval.isZero() || interval.isNegative()) {
            throw new IllegalArgumentException("interval 为正");
        }
        this.rules = List.copyOf(rules);
        this.healthSource = healthSource;
        this.interval = interval;
        this.gate = gate;
    }

    /** 通知通道（分页/webhook 归宿主）。 */
    public void onAlert(Consumer<AlertFiring> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /** 启动期校验：规则引用的机制必须存在（yml 错该红）。 */
    public void validateMechanisms() {
        Map<String, BuzhouHealth> healths = healthSource.get();
        for (AlertRule rule : rules) {
            if (!healths.containsKey(rule.mechanism())) {
                throw new IllegalArgumentException("告警规则「" + rule.name()
                        + "」引用机制 " + rule.mechanism() + " 不存在；可用机制："
                        + healths.keySet());
            }
        }
    }

    /** 评估一轮（调度器周期调；也可手动——测试/运维触发）。 */
    public void evaluate(Instant now) {
        Map<String, BuzhouHealth> healths = healthSource.get();
        for (AlertRule rule : rules) {
            BuzhouHealth health = healths.get(rule.mechanism());
            if (health == null) {
                continue; // validateMechanisms 已挡装配期；运行期机制消失静默跳过
            }
            boolean down = health.status() == BuzhouHealth.Status.DOWN;
            String key = rule.name();
            if (!down) {
                downSince.remove(key);
                if (Boolean.TRUE.equals(firing.remove(key))) {
                    notify(new AlertFiring(rule.name(), rule.mechanism(), true,
                            now, health.details()));
                }
                continue;
            }
            Instant since = downSince.computeIfAbsent(key, k -> now);
            boolean sustained = !rule.forDuration().isZero()
                    && Duration.between(since, now).compareTo(rule.forDuration()) < 0;
            if (sustained) {
                continue; // for 窗未满——flap 吸收中
            }
            if (firing.put(key, true) == null) {
                io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder
                        .metrics().counter("buzhou.alert.fired", 1, "rule", rule.name());
                notify(new AlertFiring(rule.name(), rule.mechanism(), false,
                        now, health.details()));
            }
        }
    }

    private void notify(AlertFiring firing0) {
        if (gate != null && !gate.observe(firing0)) {
            return; // 被静默/抑制吞下（门已留痕）——状态机照常，仅不通知
        }
        System.getLogger(AlertRuleEngine.class.getName()).log(
                System.Logger.Level.WARNING,
                "健康告警{0}：rule={1} mechanism={2} details={3}",
                firing0.recovered() ? "恢复" : "触发", firing0.ruleName(),
                firing0.mechanism(), firing0.details());
        if (firing0.recovered()) {
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder
                    .metrics().counter("buzhou.alert.recovered", 1, "rule", firing0.ruleName());
        }
        listeners.forEach(listener -> {
            try {
                listener.accept(firing0);
            } catch (RuntimeException e) {
                // 通知通道故障不阻断引擎（下一个告警仍要能发）
            }
        });
    }

    /** 规则集（观测面）。 */
    public List<AlertRule> rules() {
        return rules;
    }

    /** spec 345 / T681：firing 视图（规则名 → 是否 firing——只读副本，面板端点用）。 */
    public Map<String, Boolean> firingView() {
        Map<String, Boolean> view = new java.util.LinkedHashMap<>();
        firing.forEach(view::put);
        return view;
    }

    // ---- SmartLifecycle：start 期校验（晚于全部 bean 创建）+ 周期评估 ----

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            validateMechanisms(); // 启动期 fail-fast：yml 引用错机制该红
            if (gate != null) {
                gate.validateMechanisms(healthSource.get()); // 静默/抑制引用同口径校验
            }
            scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(
                    io.github.chyuan_cuihongyuan.buzhou.core.concurrent.BuzhouThreadFactory
                            .platform("buzhou-alert"));
            scheduler.scheduleAtFixedRate(() -> evaluate(Instant.now()),
                    interval.toMillis(), interval.toMillis(),
                    java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            java.util.concurrent.ScheduledExecutorService current = scheduler;
            if (current != null) {
                current.shutdownNow();
                scheduler = null;
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }
}
