package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.config.ConfigDoctor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 配置体检健康段（spec 107 §A / T391，spec 91 fog 项收口）：就绪事件跑一次
 * ConfigDoctor 并缓存报告；{@code /actuator/buzhou} 的 config-doctor 段给
 * errors/warnings/checkedKeys（findings 明细走启动日志——健康详情有界纪律）。
 * 状态：就绪前 UNKNOWN（未体检不冒充干净）；就绪后恒 UP（观测面——错误数在
 * details，DOWN 判定留给核心职能机制，error-signatures 同纪律）。
 *
 * <p><b>陈旧度（spec 142 §A / T467，Consul TTL check 借鉴）</b>：可选
 * {@code freshnessTtl}——体检报告超过 TTL 未刷新转 UNKNOWN(stale)：旧报告的
 * errors/warnings 是旧世界的快照，冒充「现在干净」是谎言；{@link #reexamine()}
 * 手动刷新面恒可用（运维 cron / 端点驱动）。默认无 TTL = 既有行为零变化。
 */
public final class ConfigDoctorHealth implements BuzhouHealth,
        ApplicationListener<ApplicationReadyEvent> {

    private final org.springframework.core.env.Environment env;
    private final java.time.Duration freshnessTtl;
    private final AtomicReference<Examined> lastReport = new AtomicReference<>();

    /** 报告 + 体检时刻（TTL 判定的事实对）。 */
    private record Examined(ConfigDoctor.DoctorReport report, java.time.Instant at) {
    }

    public ConfigDoctorHealth(org.springframework.core.env.Environment env) {
        this(env, null);
    }

    /** 带陈旧度 TTL 的构造（ttl ≤ 0 拒绝——TTL 要么有意义要么不设）。 */
    public ConfigDoctorHealth(org.springframework.core.env.Environment env,
                              java.time.Duration freshnessTtl) {
        if (freshnessTtl != null && !freshnessTtl.isPositive()) {
            throw new IllegalArgumentException("freshnessTtl must be positive: " + freshnessTtl);
        }
        this.env = env;
        this.freshnessTtl = freshnessTtl;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        reexamine();
    }

    /** 手动刷新（运维 cron/端点驱动；返回本次报告——TTL 的逃逸面）。 */
    public ConfigDoctor.DoctorReport reexamine() {
        ConfigDoctor.DoctorReport report = new ConfigDoctor().examine(env);
        lastReport.set(new Examined(report, java.time.Instant.now()));
        System.Logger logger = System.getLogger(ConfigDoctorHealth.class.getName());
        logger.log(System.Logger.Level.INFO, report.summary());
        for (ConfigDoctor.Finding finding : report.findings()) {
            logger.log(System.Logger.Level.WARNING,
                    "config-doctor " + finding.level() + " " + finding.key() + "："
                            + finding.message());
        }
        return report;
    }

    @Override
    public String mechanism() {
        return "config-doctor";
    }

    @Override
    public Status status() {
        Examined examined = lastReport.get();
        if (examined == null) {
            return Status.UNKNOWN; // 未体检（pending）
        }
        if (freshnessTtl != null && java.time.Duration.between(examined.at(),
                java.time.Instant.now()).compareTo(freshnessTtl) > 0) {
            return Status.UNKNOWN; // 体检过期（stale）：旧快照不冒充现在
        }
        return Status.UP;
    }

    @Override
    public Map<String, Object> details() {
        Examined examined = lastReport.get();
        if (examined == null) {
            return Map.of("pending", true); // 就绪事件未到（或手动构造未触发）
        }
        java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("errors", examined.report().errorCount());
        out.put("warnings", examined.report().warnCount());
        out.put("checkedKeys", examined.report().checkedKeys());
        out.put("examinedAt", examined.at().toString());
        if (freshnessTtl != null) {
            out.put("freshnessTtlMs", freshnessTtl.toMillis());
            out.put("stale", status() == Status.UNKNOWN);
        }
        return java.util.Collections.unmodifiableMap(out);
    }
}
