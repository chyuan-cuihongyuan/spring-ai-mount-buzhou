package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 启动装配摘要（spec 625 / T900，Spring Boot diagnostics report / actuator startup
 * 思想）：ApplicationReady 后一行 INFO 输出当前生效的 buzhou 面板——机制开关、
 * store 形态、模型名。运维一眼看清「这套进程装了什么」（排障/工单第一入口）。
 * opt-in：{@code buzhou.assembly-report.enabled=true}。
 */
public class BuzhouAssemblyReport implements ApplicationListener<ApplicationReadyEvent> {

    /** 摘要覆盖的机制开关键（与各模块 @ConditionalOnProperty 同名）。 */
    static final List<String> MECHANISM_KEYS = List.of(
            "buzhou.memory.enabled", "buzhou.spill.enabled", "buzhou.observability.enabled",
            "buzhou.skills.enabled", "buzhou.mcp.enabled", "buzhou.guard.enabled",
            "buzhou.tools.enabled", "buzhou.resilience.enabled",
            "buzhou.observe.otel.enabled", "buzhou.observe.dashboard.enabled");

    private final Environment env;

    public BuzhouAssemblyReport(Environment env) {
        this.env = env;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        System.getLogger(BuzhouAssemblyReport.class.getName()).log(
                System.Logger.Level.INFO, "Buzhou 装配摘要：{0}", summary(env));
    }

    /** 生效面板（测试/导出面：机制开关 + store + 模型名；缺省键显示默认值）。 */
    static Map<String, String> summary(Environment env) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String key : MECHANISM_KEYS) {
            out.put(key, env.getProperty(key, Boolean.class, defaultOf(key)).toString());
        }
        out.put("buzhou.store.type", env.getProperty("buzhou.store.type", "memory"));
        out.put("buzhou.model-name", env.getProperty("buzhou.model-name", "unknown"));
        return out;
    }

    private static boolean defaultOf(String key) {
        // otel/dashboard 默认关（safe-by-default 表），其余默认开——与各模块装配一致
        return !key.startsWith("buzhou.observe.");
    }
}
