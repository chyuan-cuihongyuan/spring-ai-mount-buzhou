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
 */
public final class ConfigDoctorHealth implements BuzhouHealth,
        ApplicationListener<ApplicationReadyEvent> {

    private final org.springframework.core.env.Environment env;
    private final AtomicReference<ConfigDoctor.DoctorReport> lastReport =
            new AtomicReference<>();

    public ConfigDoctorHealth(org.springframework.core.env.Environment env) {
        this.env = env;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        ConfigDoctor.DoctorReport report = new ConfigDoctor().examine(env);
        lastReport.set(report);
        System.Logger logger = System.getLogger(ConfigDoctorHealth.class.getName());
        logger.log(System.Logger.Level.INFO, report.summary());
        for (ConfigDoctor.Finding finding : report.findings()) {
            logger.log(System.Logger.Level.WARNING,
                    "config-doctor " + finding.level() + " " + finding.key() + "："
                            + finding.message());
        }
    }

    @Override
    public String mechanism() {
        return "config-doctor";
    }

    @Override
    public Status status() {
        return lastReport.get() == null ? Status.UNKNOWN : Status.UP;
    }

    @Override
    public Map<String, Object> details() {
        ConfigDoctor.DoctorReport report = lastReport.get();
        if (report == null) {
            return Map.of("pending", true); // 就绪事件未到（或手动构造未触发）
        }
        return Map.of(
                "errors", report.errorCount(),
                "warnings", report.warnCount(),
                "checkedKeys", report.checkedKeys());
    }
}
