package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.AgentRuntimeLifecycle;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.DefaultAgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.HarnessAssembler;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAssemblyCustomizer;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionResourceCustomizer;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MemoryViewProcessor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

/**
 * 内核自装配（spec 09 / ticket 22）。
 *
 * <p>两件套：
 * <ol>
 *   <li>按 {@code buzhou.store.type}（默认 {@code memory}）装配 {@link BuzhouStores}；
 *       jdbc/redis 实现由各自模块按 store.type 条件装配，本类只提供内存默认。</li>
 *   <li>收集容器内全部 {@link RuntimeConfig}（机制模块产出）与扩展组件 bean
 *       （{@link BuzhouHook} / {@link ToolCallback} / {@link SessionAssemblyCustomizer} /
 *       {@link SessionResourceCustomizer} / {@link MemoryViewProcessor}，供用户自定义扩展），
 *       经 {@link RuntimeConfig#merge} 合成单一 {@link AgentRuntime}（依赖 {@link ChatModel}）。</li>
 * </ol>
 *
 * <p>合并后的 {@link RuntimeConfig} 是 {@link #buzhouAgentRuntime} 方法内的局部变量，
 * <b>不</b>暴露为 bean，避免被 {@code List<RuntimeConfig>} 自收集（无环）。
 */
@AutoConfiguration
@EnableConfigurationProperties({BuzhouCoreProperties.class, BuzhouRetentionProperties.class,
        BuzhouRunawayProperties.class, BuzhouBackpressureProperties.class,
        BuzhouTokenBudgetProperties.class,
        io.github.chyuan_cuihongyuan.buzhou.core.webhook.BuzhouWebhookProperties.class,
        BuzhouToolsProperties.class, BuzhouArchiveProperties.class,
        BuzhouVirtualKeyProperties.class, BuzhouAlertProperties.class,
        SessionDisruptionBudgetProperties.class, BulkheadScalingProperties.class,
        ErrorBudgetProperties.class, ChaosProperties.class, DryRunProperties.class,
        ToolKillSwitchProperties.class, RepetitionProperties.class,
        ToolLoopProperties.class, BuzhouProbeProperties.class,
        BuzhouMessageEncryptionProperties.class, ErrorBudgetFreezeProperties.class,
        BuzhouMaintenanceProperties.class, BuzhouPromptProperties.class,
        BuzhouPromptUsageProperties.class, BuzhouTurnRateLimitProperties.class,
        BuzhouCostForecastProperties.class, BuzhouHealthTimelineProperties.class,
        BuzhouCostSpikeProperties.class,
        BuzhouLatencySloProperties.class,
        BuzhouFsckProperties.class,
        BuzhouToolDeprecationProperties.class, BuzhouEvalSamplingProperties.class,
        BuzhouPeriodBudgetProperties.class, BuzhouToolResultSchemasProperties.class,
        BuzhouConfigAuditProperties.class, BuzhouToolLaneProperties.class,
        BuzhouExperimentProperties.class,
        BuzhouErrorSamplingProperties.class})
public class BuzhouCoreAutoConfiguration {

    /**
     * spec 625 / T900：启动装配摘要（opt-in——{@code buzhou.assembly-report.enabled=true}
     * 声明即装；ApplicationReady 后一行 INFO 输出机制开关/store/模型名生效面板）。
     */
    @org.springframework.context.annotation.Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            prefix = "buzhou.assembly-report", name = "enabled", havingValue = "true")
    public BuzhouAssemblyReport buzhouAssemblyReport(org.springframework.core.env.Environment env) {
        return new BuzhouAssemblyReport(env);
    }

    /**
     * spec 333 / T658：消息静态信封加密（{@code buzhou.security.message-encryption.master-key}
     * 声明即启用——Vault transit / KMS envelope 思想：密钥不出进程、存储只见
     * 密文、AAD 绑定标识防剪贴、previous-master-key 双钥轮换窗口）。BPP 捕获
     * BuzhouStores 重建（仅换 messageStore 槽）；未配 master-key = 零行为变化。
     * 静态声明：BPP 须早于普通 bean 就绪（不被本配置类代理依赖拖晚）。
     */
    @org.springframework.context.annotation.Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            prefix = "buzhou.security.message-encryption", name = "master-key")
    public static BuzhouMessageEncryptionPostProcessor buzhouMessageEncryptionPostProcessor(
            org.springframework.core.env.Environment environment) {
        return new BuzhouMessageEncryptionPostProcessor(environment);
    }

    /**
     * spec 337 / T666：工具上下文行李（W3C Baggage——tenant/env 等路由元数据
     * 经 ToolContext 带外直达工具不进提示词）。bean 恒在（325 事故按钮同纪律：
     * 运行时 put API 必须预先在场，不依赖 yml）；yml buzhou.tools.baggage.<k>=<v>
     * 静态播种，越限值启动红（fail-fast 带修法）。
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
    public io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolBaggage buzhouToolBaggage(
            BuzhouToolsProperties properties) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolBaggage(
                properties.baggage());
    }

    /**
     * spec 318 / T628：会话扰乱预算装配（{@code buzhou.session.disruption-budget.min-available}
     * 配置即装配——K8s PDB 思想：voluntary 排水领额度，保底可用数不穿）。配置了但
     * 无会话索引（计数源）启动即红（fail-fast 带修法）。
     */
    @Bean
    @ConditionalOnProperty(prefix = "buzhou.session.disruption-budget", name = "min-available")
    public io.github.chyuan_cuihongyuan.buzhou.core.session.SessionDisruptionBudget
    buzhouSessionDisruptionBudget(
            SessionDisruptionBudgetProperties properties,
            org.springframework.beans.factory.ObjectProvider<
                    io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore> indexStore) {
        io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore index = indexStore.getIfAvailable();
        if (index == null) {
            throw new BuzhouConfigurationException(
                    "buzhou.session.disruption-budget.min-available 配置了但无会话索引（ACTIVE 计数源）",
                    "引入 store 模块（store.type 配置）或删除该配置");
        }
        return new io.github.chyuan_cuihongyuan.buzhou.core.session.SessionDisruptionBudget(
                () -> countActive(index), properties.minAvailable() == null
                        ? 0L : properties.minAvailable());
    }

    /** ACTIVE 计数（分页枚举上限 50 页——大舰队截断诚实入档）。 */
    private static long countActive(io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore index) {
        long count = 0;
        for (int page = 0; page < 50; page++) {
            var batch = index.list(new io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexQuery(
                    null, null, io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo.STATUS_ACTIVE,
                    null, null, page * 200, 200));
            count += batch.size();
            if (batch.size() < 200) {
                break;
            }
        }
        return count;
    }

    /**
     * spec 321 / T633：SLO 错误预算（{@code buzhou.error-budget.slo} 配置即装配
     * ——Google SRE burn rate：窗错误率/(1−SLO)，超阈走健康面 DOWN，312 告警
     * 引擎的 for 持续窗吸收瞬态）。hook 纯观察（order 250），健康面自动进
     * 机制集。
     */
    @Bean
    @ConditionalOnProperty(prefix = "buzhou.error-budget", name = "slo")
    public io.github.chyuan_cuihongyuan.buzhou.core.health.ErrorBudget buzhouErrorBudget(
            ErrorBudgetProperties properties) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.health.ErrorBudget(
                properties.toConfig(), java.time.Clock.systemDefaultZone());
    }

    /** spec 321：喂数 hook（BuzhouHook 自动收集进 RuntimeConfig——不挂零变化）。 */
    @Bean
    @ConditionalOnProperty(prefix = "buzhou.error-budget", name = "slo")
    public io.github.chyuan_cuihongyuan.buzhou.core.health.ErrorBudgetHook buzhouErrorBudgetHook(
            io.github.chyuan_cuihongyuan.buzhou.core.health.ErrorBudget budget) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.health.ErrorBudgetHook(budget);
    }

    /** spec 321：健康面（BuzhouHealth——DOWN=燃尽超阈 SLO 失守；无样本 UNKNOWN）。 */
    @Bean
    @ConditionalOnProperty(prefix = "buzhou.error-budget", name = "slo")
    public io.github.chyuan_cuihongyuan.buzhou.core.health.ErrorBudgetHealth buzhouErrorBudgetHealth(
            io.github.chyuan_cuihongyuan.buzhou.core.health.ErrorBudget budget) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.health.ErrorBudgetHealth(budget);
    }

    /**
     * spec 335 / T662：错误预算政策（Google SRE error budget policy——烧穿自动
     * 冻结低优先级 spawn）。{@code buzhou.backpressure.error-budget-freeze.enabled=true}
     * 才装配；无 ErrorBudget 喂数（未配 buzhou.error-budget.slo）启动红——无观察面
     * 的政策是盲动。地板槽恒供（政策驱动、gate 读取——解耦装配顺序）。
     */
    /**
     * spec 342 / T676：准入地板槽恒供（335 冻结与 342 cordon 共用——多源合成
     * 正交；单 bean 免歧义）。
     */
    @Bean
    public io.github.chyuan_cuihongyuan.buzhou.core.backpressure.SpawnAdmissionFloor
    buzhouSpawnAdmissionFloor() {
        return new io.github.chyuan_cuihongyuan.buzhou.core.backpressure.SpawnAdmissionFloor();
    }

    /** spec 348 / T688：重试预算健康面（恒 UP——拦截是保护生效；holder 空自报 UNKNOWN）。 */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
    public io.github.chyuan_cuihongyuan.buzhou.core.health.RetryBudgetHealth buzhouRetryBudgetHealth() {
        return new io.github.chyuan_cuihongyuan.buzhou.core.health.RetryBudgetHealth();
    }

    /**
     * spec 342 / T676：维护窗 cordon（K8s cordon——窗内不接新会话、在途排空）。
     * bean 恒在（325 纪律——运行时 cordon/uncordon 按钮必须预先在场）。
     * 过期窗启动 no-op。
     */
    @Bean
    public io.github.chyuan_cuihongyuan.buzhou.core.backpressure.MaintenanceCordon
    buzhouMaintenanceCordon(
            io.github.chyuan_cuihongyuan.buzhou.core.backpressure.SpawnAdmissionFloor floor,
            BuzhouMaintenanceProperties properties) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.backpressure.MaintenanceCordon(
                floor, properties.from(), properties.until(), properties.reason(),
                properties.pollInterval(), null);
    }

    @Bean
    @ConditionalOnProperty(prefix = "buzhou.backpressure.error-budget-freeze",
            name = "enabled", havingValue = "true")
    public io.github.chyuan_cuihongyuan.buzhou.core.backpressure.ErrorBudgetPolicy
    buzhouErrorBudgetPolicy(
            org.springframework.beans.factory.ObjectProvider<
                    io.github.chyuan_cuihongyuan.buzhou.core.health.ErrorBudget> budgetProvider,
            io.github.chyuan_cuihongyuan.buzhou.core.backpressure.SpawnAdmissionFloor floor,
            ErrorBudgetFreezeProperties properties) {
        io.github.chyuan_cuihongyuan.buzhou.core.health.ErrorBudget budget =
                budgetProvider.getIfAvailable();
        if (budget == null) {
            throw new BuzhouConfigurationException(
                    "buzhou.backpressure.error-budget-freeze.enabled=true 但无 ErrorBudget bean"
                            + "——先配 buzhou.error-budget.slo（无观察面的政策是盲动）",
                    "buzhou.error-budget.slo=99.9 等先行配置");
        }
        return new io.github.chyuan_cuihongyuan.buzhou.core.backpressure.ErrorBudgetPolicy(
                budget, floor, properties.interval());
    }

    /**
     * spec 322 / T635：工具混沌注入（opt-in {@code buzhou.chaos.enabled=true}
     * ——Netflix Chaos Monkey：按概率注入延迟/故障，平时演练熔断/重试预算/
     * 舱/错误预算。hook 自动收集进 RuntimeConfig（order 235 熔断前）；概率源
     * ThreadLocalRandom（装配非确定，测试注入确定源）。
     */
    @Bean
    @ConditionalOnProperty(prefix = "buzhou.chaos", name = "enabled", havingValue = "true")
    public io.github.chyuan_cuihongyuan.buzhou.core.exec.ChaosMonkeyHook buzhouChaosMonkeyHook(
            ChaosProperties properties) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.exec.ChaosMonkeyHook(
                properties.latencyPercent() == null ? 0.0 : properties.latencyPercent(),
                properties.latencyMillis() == null ? 0L : properties.latencyMillis(),
                properties.exceptionPercent() == null ? 0.0 : properties.exceptionPercent(),
                properties.tools() == null ? java.util.Set.of()
                        : java.util.Set.copyOf(properties.tools()),
                true, java.util.concurrent.ThreadLocalRandom.current()::nextDouble);
    }

    /**
     * spec 323 / T637：干跑拦截（opt-in {@code buzhou.dry-run.enabled=true}
     * ——Terraform plan 思想：拦入计划不执行，计划面可审阅；非错误标记，
     * 熔断/错误预算不被演练污染）。hook 自动收集（order 290 HITL 前）；
     * include 清单空 = 全量拦。
     */
    @Bean
    @ConditionalOnProperty(prefix = "buzhou.dry-run", name = "enabled", havingValue = "true")
    public io.github.chyuan_cuihongyuan.buzhou.core.exec.DryRunHook buzhouDryRunHook(
            DryRunProperties properties) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.exec.DryRunHook(
                properties.tools() == null ? java.util.Set.of()
                        : java.util.Set.copyOf(properties.tools()),
                true);
    }

    /**
     * spec 325 / T641：工具紧急停用（LaunchDarkly kill switch / K8s cordon）：
     * <b>装配恒在</b>（空集直通零变化——事故按钮必须预先存在才有用）；hook
     * 自动收集（order 15）；yml 列表启动预停用且是刷新事件的事实源。
     */
    @Bean
    public io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolKillSwitchHook buzhouToolKillSwitchHook(
            ToolKillSwitchProperties properties) {
        io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolKillSwitchHook hook =
                new io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolKillSwitchHook();
        if (properties.tools() != null && !properties.tools().isEmpty()) {
            hook.disableTools(java.util.Set.copyOf(properties.tools()));
        }
        return hook;
    }

    /** spec 325：停用集热重载（320 刷新事件通道——yml 整体覆盖）。 */
    @Bean
    public io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolKillSwitchHotReload
    buzhouToolKillSwitchHotReload(
            io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolKillSwitchHook hook,
            org.springframework.core.env.Environment env) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolKillSwitchHotReload(
                hook, env);
    }

    /**
     * spec 326 / T643：轮次重复检测（{@code buzhou.runaway.repetition.window}
     * 配置即装配——LLM 打转 content rot 早信号：afterModel 喂文本，相邻
     * Jaccard run 达窗 fire；unstick=true 时 block 回填解困指令替换复读输出）。
     */
    @Bean
    @ConditionalOnProperty(prefix = "buzhou.runaway.repetition", name = "window")
    public io.github.chyuan_cuihongyuan.buzhou.core.runaway.RepetitionDetectorHook
    buzhouRepetitionDetectorHook(RepetitionProperties properties) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.runaway.RepetitionDetectorHook(
                properties.window(),
                properties.similarityPercent() == null ? 80.0 : properties.similarityPercent(),
                Boolean.TRUE.equals(properties.unstick()));
    }

    /**
     * spec 327 / T645：工具循环断路器（{@code buzhou.runaway.tool-loop.window}
     * 配置即装配且<b>装配即干预</b>——同工具同参数连续达窗 block 带三选一
     * 出路；与熔断正交：熔断按错误率，本闸按调用形态）。hook 自动收集
     * （order 245 熔断后）。
     */
    @Bean
    @ConditionalOnProperty(prefix = "buzhou.runaway.tool-loop", name = "window")
    public io.github.chyuan_cuihongyuan.buzhou.core.runaway.ToolLoopBreakerHook
    buzhouToolLoopBreakerHook(ToolLoopProperties properties) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.runaway.ToolLoopBreakerHook(
                properties.window());
    }

    /**
     * spec 312 / T616：健康告警规则装配（{@code buzhou.alert.rules} 声明即装配；
     * 无规则 = NullBean 零变化）。健康 bean 集在 start() 期解析（SmartLifecycle 晚于
     * 全部 bean 创建——规避同配置类条件可见性坑）；引用缺失机制启动即红。
     */
    @Bean
    @org.springframework.context.annotation.Conditional(
            BuzhouCoreAutoConfiguration.AlertRulesPresentCondition.class)
    public io.github.chyuan_cuihongyuan.buzhou.core.health.AlertRuleEngine buzhouAlertRuleEngine(
            BuzhouAlertProperties properties,
            org.springframework.beans.factory.ObjectProvider<
                    io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth> healthBeans,
            org.springframework.beans.factory.ObjectProvider<
                    io.github.chyuan_cuihongyuan.buzhou.core.health.AlertGate> gateProvider) {
        java.util.List<io.github.chyuan_cuihongyuan.buzhou.core.health.AlertRuleEngine.AlertRule> rules =
                properties.rules().stream()
                        .map(r -> new io.github.chyuan_cuihongyuan.buzhou.core.health
                                .AlertRuleEngine.AlertRule(r.name(), r.mechanism(), r.forDuration(),
                                r.annotations())) // spec 347：注解随发
                        .toList();
        return new io.github.chyuan_cuihongyuan.buzhou.core.health.AlertRuleEngine(rules,
                () -> {
                    java.util.Map<String, io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth> map =
                            new java.util.LinkedHashMap<>();
                    healthBeans.orderedStream()
                            .forEach(health -> map.put(health.mechanism(), health));
                    return map;
                }, properties.interval(), gateProvider.getIfAvailable());
    }

    /**
     * spec 414 / T720：配置漂移审计（ArgoCD drift detection 借鉴）。
     * {@code buzhou.config-audit.enabled=true} 声明即装配：周期快照 diff，
     * 变更 WARN 日志留痕（宿主可注入增强 listener——bean 可覆盖）。
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(
            io.github.chyuan_cuihongyuan.buzhou.core.config.ConfigDriftAuditor.class)
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            prefix = "buzhou.config-audit", name = "enabled", havingValue = "true")
    public io.github.chyuan_cuihongyuan.buzhou.core.config.ConfigDriftAuditor
    buzhouConfigDriftAuditor(BuzhouConfigAuditProperties properties,
            org.springframework.core.env.Environment environment) {
        System.Logger logger = System.getLogger("buzhou.config-drift");
        return new io.github.chyuan_cuihongyuan.buzhou.core.config.ConfigDriftAuditor(
                environment, properties.interval(), changes -> {
                    for (var c : changes) {
                        logger.log(System.Logger.Level.WARNING,
                                "配置漂移：{0}: {1} -> {2}", c.key(), c.from(), c.to());
                    }
                });
    }

    /**
     * spec 410 / T712：共享事实库（mem0 共享记忆+隔离借鉴——deny-by-default）。
     * bean 恒在（空库零行为——宿主程序面注入使用；进程内诚实边界重启清零）。
     */
    @Bean
    public io.github.chyuan_cuihongyuan.buzhou.core.fact.SharedFactStore buzhouSharedFactStore() {
        return new io.github.chyuan_cuihongyuan.buzhou.core.fact.InMemorySharedFactStore();
    }

    /**
     * spec 420 / T732：工具目录 lint（ESLint 构建期 lint 借鉴——只报不改）。
     * {@code buzhou.tools.catalog-lint.enabled=true} 声明即装配：装配期
     * wrapToolCallbacks 一遍扫三规则（名字约定/描述长度/跨源重名）。
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            prefix = "buzhou.tools.catalog-lint", name = "enabled", havingValue = "true")
    public RuntimeConfig buzhouToolCatalogLintRuntimeConfig() {
        return new RuntimeConfig(java.util.List.of(), java.util.Set.of(), java.util.Set.of(),
                null, java.util.List.of(), java.util.Map.of(), java.util.List.of(),
                java.util.List.of(ctx -> {
                    io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolCatalogLinter linter =
                            new io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolCatalogLinter(
                                    ctx::emitEvent);
                    java.util.List<org.springframework.ai.tool.ToolCallback> seen =
                            new java.util.ArrayList<>();
                    java.util.List<io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolCatalogLinter.Finding>
                            findings = new java.util.ArrayList<>();
                    ctx.wrapToolCallbacks(cb -> {
                        findings.addAll(linter.lint(cb, seen));
                        seen.add(cb);
                        return cb; // 只报不改
                    });
                    linter.announce(ctx.sessionId(), findings);
                }),
                null);
    }

    /**
     * spec 409 / T710：工具结果 schema 校验（MCP outputSchema 借鉴——复用
     * ToolArgsValidator 同一校验器）。{@code buzhou.tools.result-schemas.<name>}
     * 声明即装配（Binder 预绑判 map 非空——406 同法）。
     */
    @Bean
    @org.springframework.context.annotation.Conditional(
            BuzhouCoreAutoConfiguration.ResultSchemasPresentCondition.class)
    public RuntimeConfig buzhouToolResultSchemasRuntimeConfig(
            org.springframework.core.env.Environment env) {
        // spec 531 装配审计修复：单 Map 组件 record 构造绑定在 prefix.<组件名> 子路径，
        // 根前缀 yml 必须根绑定直读（原 properties 注入绑空——静默 no-op）
        java.util.Map<String, String> schemas = org.springframework.boot.context.properties.bind.Binder
                .get(env)
                .bind("buzhou.tools.result-schemas",
                        org.springframework.boot.context.properties.bind.Bindable
                                .mapOf(String.class, String.class))
                .orElse(java.util.Map.of());
        var hook = new io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolResultSchemaHook(
                schemas);
        return new RuntimeConfig(java.util.List.of(hook), java.util.Set.of(), java.util.Set.of(),
                null, java.util.List.of(), java.util.Map.of(), java.util.List.of(),
                java.util.List.of(), null);
    }

    /**
     * spec 505 / T761：在线实验确定性分桶（GrowthBook/Statsig 思想）——
     * buzhou.experiments.<experiment>.<variant> 权重声明即装配（Binder
     * 预绑判 map 非空——409 同法）。曝光统计 experiment×variant 有界。
     */
    @Bean
    @org.springframework.context.annotation.Conditional(
            BuzhouCoreAutoConfiguration.ExperimentPresentCondition.class)
    public io.github.chyuan_cuihongyuan.buzhou.core.experiment.ExperimentBucketer
    buzhouExperimentBucketer(org.springframework.core.env.Environment env) {
        // spec 531 装配审计修复：根绑定直读（原 properties 注入绑空——实验表静默空）
        java.util.Map<String, Object> raw = org.springframework.boot.context.properties.bind.Binder
                .get(env)
                .bind("buzhou.experiments",
                        org.springframework.boot.context.properties.bind.Bindable
                                .mapOf(String.class, Object.class))
                .orElse(java.util.Map.of());
        // 根绑定 Object 值为嵌套形态 {experiment: {variant: weight}}
        java.util.Map<String, java.util.Map<String, Integer>> experiments = new java.util.LinkedHashMap<>();
        raw.forEach((experiment, variants) -> {
            if (variants instanceof java.util.Map<?, ?> variantMap) {
                java.util.Map<String, Integer> weights = new java.util.LinkedHashMap<>();
                variantMap.forEach((variant, weight) -> {
                    // Object 绑定的叶子值可能是 String（属性源原文）或 Number——都接受
                    if (weight instanceof Number number) {
                        weights.put(String.valueOf(variant), number.intValue());
                    } else if (weight instanceof String text && !text.isBlank()) {
                        try {
                            weights.put(String.valueOf(variant), Integer.parseInt(text.trim()));
                        } catch (NumberFormatException ignored) {
                            // 非整型权重跳过（观测面不炸装配）
                        }
                    }
                });
                if (!weights.isEmpty()) {
                    experiments.put(experiment, weights);
                }
            }
        });
        return new io.github.chyuan_cuihongyuan.buzhou.core.experiment.ExperimentBucketer(
                experiments);
    }

    /**
     * spec 530 / T811：per-model 预算闸（budget 族扩散——ModelCostLedger
     * 记账面 vs per-model 预算，耗尽 beforeModel 拦截）。map 非空才装配；
     * 以记账面为准（未喂账恒放行）。
     */
    @Bean
    @org.springframework.context.annotation.Conditional(
            BuzhouCoreAutoConfiguration.ModelBudgetPresentCondition.class)
    public io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig
    buzhouModelBudgetRuntimeConfig(org.springframework.core.env.Environment env) {
        // 单 Map 组件 record 构造绑定在 prefix.<组件名> 子路径——根前缀 yml 会绑空
        //（524/530 装配审计结论）；此处直接根绑定（与条件判定同一读法）
        java.util.Map<String, Long> budgets = org.springframework.boot.context.properties.bind.Binder
                .get(env)
                .bind("buzhou.budget.model-budget",
                        org.springframework.boot.context.properties.bind.Bindable
                                .mapOf(String.class, Long.class))
                .orElse(java.util.Map.of());
        String modelName = env.getProperty("buzhou.model-name", "unknown");
        io.github.chyuan_cuihongyuan.buzhou.core.budget.ModelBudgetGate gate =
                new io.github.chyuan_cuihongyuan.buzhou.core.budget.ModelBudgetGate(
                        budgets, modelName,
                        io.github.chyuan_cuihongyuan.buzhou.core.budget.ModelCostLedger.global());
        return io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.hooks(
                java.util.List.of(gate));
    }

    /** spec 548 / T827：fsck 巡检健康面（观测面恒 UP——findings 是数据需关注非进程故障）。 */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            prefix = "buzhou.fsck", name = "enabled", havingValue = "true")
    public io.github.chyuan_cuihongyuan.buzhou.core.health.StoreFsckHealth buzhouStoreFsckHealth(
            io.github.chyuan_cuihongyuan.buzhou.core.cleanup.StoreFsckHousekeeper keeper) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.health.StoreFsckHealth(keeper);
    }

    /** spec 530：budgets map 非空才装配（Binder 预绑判定）。 */
    static final class ModelBudgetPresentCondition
            implements org.springframework.context.annotation.Condition {
        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext context,
                org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            try {
                return org.springframework.boot.context.properties.bind.Binder
                        .get(context.getEnvironment())
                        .bind("buzhou.budget.model-budget",
                                org.springframework.boot.context.properties.bind.Bindable
                                        .mapOf(String.class, Long.class))
                        .map(m -> {
                            return !m.isEmpty();
                        }).orElse(false);
            } catch (Exception e) {
                return false;
            }
        }
    }

    /** spec 505：experiments map 非空才装配（Binder 预绑判定）。 */
    static final class ExperimentPresentCondition
            implements org.springframework.context.annotation.Condition {
        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext context,
                org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            try {
                return org.springframework.boot.context.properties.bind.Binder
                        .get(context.getEnvironment())
                        .bind("buzhou.experiments",
                                org.springframework.boot.context.properties.bind.Bindable
                                        .mapOf(String.class, Object.class))
                        .map(m -> !m.isEmpty()).orElse(false);
            } catch (Exception e) {
                return false;
            }
        }
    }

    /** spec 409：result-schemas map 非空才装配（Binder 预绑判定）。 */
    static final class ResultSchemasPresentCondition
            implements org.springframework.context.annotation.Condition {
        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext context,
                org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            try {
                return org.springframework.boot.context.properties.bind.Binder
                        .get(context.getEnvironment())
                        .bind("buzhou.tools.result-schemas",
                                org.springframework.boot.context.properties.bind.Bindable
                                        .mapOf(String.class, String.class))
                        .map(m -> !m.isEmpty()).orElse(false);
            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * spec 408 / T708：日历周期预算（AWS Budgets calendar period 借鉴——翻页
     * = 换 tag 隐式重置）。{@code buzhou.budget.period.enabled=true} 声明即挂；
     * 两 limit 均未配 fail-fast（开预算闸却没限额是配置错误）。价目复用
     * buzhou.token-budget.pricing 单一事实源（无价目成本轨记 0——诚实）。
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            prefix = "buzhou.budget.period", name = "enabled", havingValue = "true")
    public io.github.chyuan_cuihongyuan.buzhou.core.budget.PeriodBudgetHook buzhouPeriodBudgetHook(
            BuzhouPeriodBudgetProperties properties,
            io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores stores,
            BuzhouTokenBudgetProperties tokenBudgetProps,
            org.springframework.core.env.Environment env) {
        if ((properties.tokensLimit() == null || properties.tokensLimit() <= 0)
                && (properties.costMicroUsdLimit() == null || properties.costMicroUsdLimit() <= 0)) {
            throw new BuzhouConfigurationException(
                    "buzhou.budget.period.enabled=true 但 tokens-limit/cost-micro-usd-limit 均未配",
                    "至少声明一个正限额（翻页自动重置——unit=" + properties.unit() + "）");
        }
        java.util.Map<String, io.github.chyuan_cuihongyuan.buzhou.core.budget.PeriodBudgetHook.Pricing>
                pricing = new java.util.LinkedHashMap<>();
        if (tokenBudgetProps.pricing() != null) {
            tokenBudgetProps.pricing().forEach((model, price) -> pricing.put(model,
                    new io.github.chyuan_cuihongyuan.buzhou.core.budget.PeriodBudgetHook.Pricing(
                            price.inputPerMillion(), price.outputPerMillion())));
        }
        return new io.github.chyuan_cuihongyuan.buzhou.core.budget.PeriodBudgetHook(
                stores.sessionStateStore(), properties.unit(), properties.tokensLimit(),
                properties.costMicroUsdLimit(), properties.warningPercent(), pricing,
                env.getProperty("buzhou.model-name", "unknown"), null);
    }

    /** spec 408：hook 挂 RuntimeConfig（健康面 419 独立 bean 共享 hook 实例）。 */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(
            io.github.chyuan_cuihongyuan.buzhou.core.budget.PeriodBudgetHook.class)
    public RuntimeConfig buzhouPeriodBudgetRuntimeConfig(
            io.github.chyuan_cuihongyuan.buzhou.core.budget.PeriodBudgetHook hook) {
        return new RuntimeConfig(java.util.List.of(hook), java.util.Set.of(), java.util.Set.of(),
                null, java.util.List.of(), java.util.Map.of(), java.util.List.of(),
                java.util.List.of(), null);
    }

    /**
     * spec 419 / T730：周期预算健康面（与 period.enabled 同键——属性条件，
     * 312 注记口径）：恒 UP 观测面（耗尽由闸拦截），details 双轨进度+
     * exhausted+resetsAt 回血时刻。
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            prefix = "buzhou.budget.period", name = "enabled", havingValue = "true")
    public io.github.chyuan_cuihongyuan.buzhou.core.health.PeriodBudgetHealth
    buzhouPeriodBudgetHealth(BuzhouPeriodBudgetProperties properties,
            io.github.chyuan_cuihongyuan.buzhou.core.budget.PeriodBudgetHook hook) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.health.PeriodBudgetHealth(
                hook, properties.unit(), properties.tokensLimit(), properties.costMicroUsdLimit(),
                java.time.Clock.systemUTC());
    }

    /**
     * spec 407 / T706：EvalDatasetStore bean（采样声明即暴露——宿主 createDataset
     * 建集用；集必须预建，采样不建集）。spec 423：条件放宽为「任一采样
     * enabled」——error-only 宿主（只开错误偏向采样）不必开基础采样。
     */
    @Bean
    @org.springframework.context.annotation.Conditional(
            BuzhouCoreAutoConfiguration.AnySamplingEnabledCondition.class)
    public io.github.chyuan_cuihongyuan.buzhou.core.eval.EvalDatasetStore buzhouEvalDatasetStore(
            io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores stores) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.eval.EvalDatasetStore(
                stores.sessionStateStore());
    }

    /** spec 423：基础采样或错误采样任一 enabled=true（单 store 不双 bean）。 */
    static final class AnySamplingEnabledCondition
            implements org.springframework.context.annotation.Condition {
        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext context,
                org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            return "true".equalsIgnoreCase(context.getEnvironment()
                    .getProperty("buzhou.eval.sampling.enabled"))
                    || "true".equalsIgnoreCase(context.getEnvironment()
                    .getProperty("buzhou.eval.error-sampling.enabled"));
        }
    }

    /**
     * spec 407 / T706：在线采样入评测集（Honeycomb head-based deterministic
     * sampling 借鉴）。{@code buzhou.eval.sampling.enabled=true} 声明即挂
     * afterTurn 尾观察 hook（确定性采样 + fail-soft 入集）。
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            prefix = "buzhou.eval.sampling", name = "enabled", havingValue = "true")
    public RuntimeConfig buzhouEvalSamplingRuntimeConfig(
            BuzhouEvalSamplingProperties properties,
            io.github.chyuan_cuihongyuan.buzhou.core.eval.EvalDatasetStore datasetStore) {
        var hook = new io.github.chyuan_cuihongyuan.buzhou.core.eval.TurnSamplerHook(
                datasetStore,
                new io.github.chyuan_cuihongyuan.buzhou.core.eval.TurnSamplerHook.Policy(
                        properties.dataset(), properties.ratePercent(), properties.minInputChars()));
        return new RuntimeConfig(java.util.List.of(hook), java.util.Set.of(), java.util.Set.of(),
                null, java.util.List.of(), java.util.Map.of(), java.util.List.of(),
                java.util.List.of(), null);
    }

    /**
     * spec 423 / T738：错误偏向采样（OTel tail_sampling「ERROR 全保」借鉴）。
     * {@code buzhou.eval.error-sampling.enabled=true} 声明即装配：每会话经
     * assemblyCustomizer 注册 {@code TurnErrorSampler} 观察者（错误轮不走
     * afterTurn——观察者缝采错；sessionId 取自装配 ctx 保采样键确定性）。
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            prefix = "buzhou.eval.error-sampling", name = "enabled", havingValue = "true")
    public RuntimeConfig buzhouErrorSamplingRuntimeConfig(
            BuzhouErrorSamplingProperties properties,
            io.github.chyuan_cuihongyuan.buzhou.core.eval.EvalDatasetStore datasetStore) {
        var policy = new io.github.chyuan_cuihongyuan.buzhou.core.eval.TurnErrorSampler.Policy(
                properties.dataset(), properties.errorRatePercent(), properties.minInputChars());
        return new RuntimeConfig(java.util.List.of(), java.util.Set.of(), java.util.Set.of(),
                null, java.util.List.of(), java.util.Map.of(), java.util.List.of(),
                java.util.List.of(ctx -> ctx.addObserver(
                        new io.github.chyuan_cuihongyuan.buzhou.core.eval.TurnErrorSampler(
                                datasetStore, policy, ctx.sessionId()))),
                null);
    }

    /**
     * spec 406 / T704：工具退役通告（K8s API deprecation 借鉴——通告随定义）。
     * {@code buzhou.tools.deprecated.<name>.*} 声明即装配：customizer 经
     * wrapToolCallbacks 名匹配包装（描述前缀模型可见 + 调用事件/计数）；
     * 空表/未命中零变化。
     */
    @Bean
    @org.springframework.context.annotation.Conditional(
            BuzhouCoreAutoConfiguration.ToolDeprecationPresentCondition.class)
    public RuntimeConfig buzhouToolDeprecationRuntimeConfig(org.springframework.core.env.Environment env) {
        // spec 531 装配审计修复：根绑定直读（原 properties 注入绑空——静默 no-op）
        java.util.Map<String, BuzhouToolDeprecationProperties.Spec> declaredSpecs =
                org.springframework.boot.context.properties.bind.Binder
                        .get(env)
                        .bind("buzhou.tools.deprecated",
                                org.springframework.boot.context.properties.bind.Bindable
                                        .mapOf(String.class, BuzhouToolDeprecationProperties.Spec.class))
                        .orElse(java.util.Map.of());
        java.util.Map<String, io.github.chyuan_cuihongyuan.buzhou.core.exec.DeprecatedToolCallback.Deprecation>
                declared = new java.util.LinkedHashMap<>();
        declaredSpecs.forEach((name, spec) -> declared.put(name,
                new io.github.chyuan_cuihongyuan.buzhou.core.exec.DeprecatedToolCallback.Deprecation(
                        spec.since(), spec.removalIn(), spec.successor(), spec.message())));
        return new RuntimeConfig(java.util.List.of(), java.util.Set.of(), java.util.Set.of(),
                null, java.util.List.of(), java.util.Map.of(), java.util.List.of(),
                java.util.List.of(ctx -> ctx.wrapToolCallbacks(cb -> {
                    var spec = declared.get(cb.getToolDefinition().name());
                    return spec == null ? cb
                            : new io.github.chyuan_cuihongyuan.buzhou.core.exec.DeprecatedToolCallback(
                                    cb, spec, ctx::emitEvent);
                })),
                null);
    }

    /** spec 406：deprecated map 非空才装配（Binder 预绑判定——312 同法）。 */
    static final class ToolDeprecationPresentCondition
            implements org.springframework.context.annotation.Condition {
        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext context,
                org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            try {
                return org.springframework.boot.context.properties.bind.Binder
                        .get(context.getEnvironment())
                        .bind("buzhou.tools.deprecated",
                                org.springframework.boot.context.properties.bind.Bindable
                                        .mapOf(String.class, BuzhouToolDeprecationProperties.Spec.class))
                        .map(m -> !m.isEmpty()).orElse(false);
            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * spec 422 / T736：工具泳道优先级装配（Envoy priority levels 接线——411
     * 原语收口）。{@code buzhou.tool-lanes.lanes} 声明即装配：customizer 经
     * wrapToolCallbacks 名匹配 → {@code PriorityLaneToolCallback}（共享
     * {@code ToolLaneRegistry.priorityLane} 命名单例——许可跨会话共享）；
     * 未命中工具零包装。tools 引用未声明泳道启动即红（fail-fast——拼错名
     * 不拖到首次调用）。
     */
    @Bean
    @org.springframework.context.annotation.Conditional(
            BuzhouCoreAutoConfiguration.ToolLanePresentCondition.class)
    public RuntimeConfig buzhouToolLaneRuntimeConfig(BuzhouToolLaneProperties properties) {
        properties.tools().forEach((toolName, binding) -> {
            if (!properties.lanes().containsKey(binding.lane())) {
                throw new IllegalArgumentException("buzhou.tool-lanes.tools." + toolName
                        + " 引用未声明泳道「" + binding.lane() + "」——先在 buzhou.tool-lanes.lanes 声明");
            }
        });
        java.util.Map<String, BuzhouToolLaneProperties.ToolBinding> bindings = properties.tools();
        java.util.Map<String, BuzhouToolLaneProperties.LaneSpec> lanes = properties.lanes();
        io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolLaneRegistry registry =
                new io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolLaneRegistry();
        return new RuntimeConfig(java.util.List.of(), java.util.Set.of(), java.util.Set.of(),
                null, java.util.List.of(), java.util.Map.of(), java.util.List.of(),
                java.util.List.of(ctx -> ctx.wrapToolCallbacks(cb -> {
                    BuzhouToolLaneProperties.ToolBinding binding =
                            bindings.get(cb.getToolDefinition().name());
                    if (binding == null) {
                        return cb;
                    }
                    BuzhouToolLaneProperties.LaneSpec spec = lanes.get(binding.lane());
                    return new io.github.chyuan_cuihongyuan.buzhou.core.exec.PriorityLaneToolCallback(
                            cb, registry.priorityLane(binding.lane(), spec.permits()),
                            binding.priority(), spec.acquireTimeout());
                })),
                null);
    }

    /** spec 422：lanes map 非空才装配（Binder 预绑判定——406 同法）。 */
    static final class ToolLanePresentCondition
            implements org.springframework.context.annotation.Condition {
        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext context,
                org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            try {
                return org.springframework.boot.context.properties.bind.Binder
                        .get(context.getEnvironment())
                        .bind("buzhou.tool-lanes.lanes",
                                org.springframework.boot.context.properties.bind.Bindable
                                        .mapOf(String.class, BuzhouToolLaneProperties.LaneSpec.class))
                        .map(m -> !m.isEmpty()).orElse(false);
            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * spec 425 / T742：轮次限速（nginx token bucket 借鉴）。{@code buzhou.
     * ratelimit.turns.burst} 与 {@code permits-per-minute} 双声明即装配
     * （默认键 sessionId——单会话频次帽；租户整体帽由宿主手工构造常量键
     * hook）；缺任一不装配零行为。
     */
    @Bean
    @org.springframework.context.annotation.Conditional(
            BuzhouCoreAutoConfiguration.TurnRateLimitPresentCondition.class)
    public RuntimeConfig buzhouTurnRateLimitRuntimeConfig(BuzhouTurnRateLimitProperties properties) {
        var hook = new io.github.chyuan_cuihongyuan.buzhou.core.ratelimit.TurnRateLimitHook(
                new io.github.chyuan_cuihongyuan.buzhou.core.ratelimit.TurnRateLimitHook.Policy(
                        properties.burst(), properties.permitsPerMinute()));
        return new RuntimeConfig(java.util.List.of(hook), java.util.Set.of(), java.util.Set.of(),
                null, java.util.List.of(), java.util.Map.of(), java.util.List.of(),
                java.util.List.of(), null);
    }

    /** spec 425：burst 与 permits-per-minute 双声明才装配（Binder 预绑判定——406 同法）。 */
    static final class TurnRateLimitPresentCondition
            implements org.springframework.context.annotation.Condition {
        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext context,
                org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            try {
                return org.springframework.boot.context.properties.bind.Binder
                        .get(context.getEnvironment())
                        .bind("buzhou.ratelimit.turns", BuzhouTurnRateLimitProperties.class)
                        .map(p -> p.burst() != null && p.permitsPerMinute() != null)
                        .orElse(false);
            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * spec 405 / T702：健康时间线 JSONL 导出（{@code buzhou.health.timeline.export-path}
     * 声明即装配；打开失败 fail-fast——坏路径该红）。逐变迁追加、每行 flush、
     * IO 失败吞+计数（旁路语义）。
     */
    @Bean(destroyMethod = "close")
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            prefix = "buzhou.health.timeline", name = "export-path")
    public io.github.chyuan_cuihongyuan.buzhou.core.health.HealthTimelineJsonl
    buzhouHealthTimelineJsonl(BuzhouHealthTimelineProperties properties)
            throws java.io.IOException {
        // spec 643 / T936：轮转档位 yml 透传（缺省默认 64MB×3；显式 ≤0 = 关）
        // spec 729 / T1009：压缩线透传（export-compress-from ≥2；缺省 0 = 关）
        return new io.github.chyuan_cuihongyuan.buzhou.core.health.HealthTimelineJsonl(
                java.nio.file.Path.of(properties.exportPath()),
                properties.effectiveExportMaxBytes(), properties.effectiveExportMaxHistory(),
                properties.effectiveExportCompressFrom());
    }

    /**
     * spec 405 / T702：健康时间线轮询记录器（{@code buzhou.health.timeline.enabled=true}
     * 声明即装配）：周期轮询 health beans（与 312 告警引擎同源 supplier 口径）→
     * diff 入环 + 可选 JSONL sink。独立调度——关时间线不影响告警引擎。
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            prefix = "buzhou.health.timeline", name = "enabled", havingValue = "true")
    public io.github.chyuan_cuihongyuan.buzhou.core.health.HealthTimelineRecorder
    buzhouHealthTimelineRecorder(
            BuzhouHealthTimelineProperties properties,
            org.springframework.beans.factory.ObjectProvider<
                    io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth> healthBeans,
            org.springframework.beans.factory.ObjectProvider<
                    io.github.chyuan_cuihongyuan.buzhou.core.health.HealthTimelineJsonl> jsonl) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.health.HealthTimelineRecorder(
                () -> {
                    java.util.Map<String, io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth> map =
                            new java.util.LinkedHashMap<>();
                    healthBeans.orderedStream()
                            .forEach(health -> map.put(health.mechanism(), health));
                    return map;
                },
                properties.interval(),
                jsonl.getIfAvailable(),
                new io.github.chyuan_cuihongyuan.buzhou.core.health.HealthTimeline(
                        properties.capacity()));
    }

    /**
     * spec 405 / T702：健康时间线端点 {@code /actuator/buzhou-timeline}——
     * 近期变迁 + per-mechanism 计数（抖动识别面）。与记录器同属性键装配
     * （@ConditionalOnBean 同配置类可见性坑——312 注记，属性条件无序依赖）。
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            prefix = "buzhou.health.timeline", name = "enabled", havingValue = "true")
    public io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouTimelineEndpoint
    buzhouTimelineEndpoint(
            org.springframework.beans.factory.ObjectProvider<
                    io.github.chyuan_cuihongyuan.buzhou.core.health.HealthTimelineRecorder> recorderProvider) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouTimelineEndpoint(
                recorderProvider.getIfAvailable());
    }

    /**
     * spec 403 / T698：成本预测健康面（AWS Budgets forecast 借鉴——窗口速率 ×
     * 水平线线性外推）。{@code buzhou.budget.forecast.enabled=true} 声明即装配：
     * 订阅 ModelCostLedger 全局记账（监听缝单点喂数）；恒 UP（预测面——超预算是
     * 预测不是事故）；budget-micro-usd ≤ 0 半配置 → UNKNOWN（速率仍可见）。
     * 重启历史清零（进程内观察面口径——诚实边界）。
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            prefix = "buzhou.budget.forecast", name = "enabled", havingValue = "true")
    public io.github.chyuan_cuihongyuan.buzhou.core.health.CostForecastHealth buzhouCostForecastHealth(
            BuzhouCostForecastProperties properties) {
        io.github.chyuan_cuihongyuan.buzhou.core.budget.SpendRateRing ring =
                new io.github.chyuan_cuihongyuan.buzhou.core.budget.SpendRateRing();
        io.github.chyuan_cuihongyuan.buzhou.core.budget.ModelCostLedger.global()
                .addListener(cost -> ring.record(cost.microUsd()));
        return new io.github.chyuan_cuihongyuan.buzhou.core.health.CostForecastHealth(
                ring, properties.window(), properties.horizon(), properties.budgetMicroUsd());
    }

    /**
     * spec 508 / T767：成本异常尖峰检测（Prometheus/Istio 滚动基线 z-score——
     * 与 403 forecast 互补：趋势 vs 突刺）。enabled=true 装配：监听
     * ModelCostLedger 全局记账单点喂数（403 同缝）；只检测不拦截。
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            prefix = "buzhou.budget.spike", name = "enabled", havingValue = "true")
    public io.github.chyuan_cuihongyuan.buzhou.core.budget.CostSpikeDetector
    buzhouCostSpikeDetector(BuzhouCostSpikeProperties properties) {
        io.github.chyuan_cuihongyuan.buzhou.core.budget.SpendRateRing ring =
                new io.github.chyuan_cuihongyuan.buzhou.core.budget.SpendRateRing(
                        properties.baselineBuckets() + 2, java.time.Clock.systemUTC());
        io.github.chyuan_cuihongyuan.buzhou.core.budget.CostSpikeDetector detector =
                new io.github.chyuan_cuihongyuan.buzhou.core.budget.CostSpikeDetector(
                        ring, properties.baselineBuckets(), properties.minSamples(),
                        properties.zThreshold(), properties.floorMicroUsd(),
                        properties.cooldown(), java.time.Clock.systemUTC());
        io.github.chyuan_cuihongyuan.buzhou.core.budget.ModelCostLedger.global()
                .addListener(cost -> {
                    try {
                        detector.record(cost.microUsd());
                    } catch (RuntimeException ignored) {
                        // 喂数隔离——记账路径不因检测面异常中断
                    }
                });
        return detector;
    }

    /**
     * spec 509 / T769：时延 SLO 燃尽（Google SRE——321 时延维度扩散：坏事件
     * =elapsed>threshold）。enabled=true 装配 RuntimeConfig.hooks（默认关——
     * 完全零钩子零开销）。
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            prefix = "buzhou.latency-slo", name = "enabled", havingValue = "true")
    public io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig
    buzhouLatencySloRuntimeConfig(BuzhouLatencySloProperties properties) {
        io.github.chyuan_cuihongyuan.buzhou.core.health.ErrorBudget budget =
                new io.github.chyuan_cuihongyuan.buzhou.core.health.ErrorBudget(
                        new io.github.chyuan_cuihongyuan.buzhou.core.health.ErrorBudget.Config(
                                properties.sloPercent(), properties.burnRateThreshold(),
                                io.github.chyuan_cuihongyuan.buzhou.core.health.ErrorBudget.Config
                                        .DEFAULT_BUCKETS,
                                properties.window(), properties.minSamples()),
                        java.time.Clock.systemUTC());
        io.github.chyuan_cuihongyuan.buzhou.core.health.LatencySloMonitor monitor =
                new io.github.chyuan_cuihongyuan.buzhou.core.health.LatencySloMonitor(
                        properties.thresholdMillis(), budget);
        return io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.hooks(
                java.util.List.of(monitor));
    }

    /**
     * spec 424 / T740：提示词使用统计 holder（bean 恒在——闲置零成本；
     * 装饰器记账的落点、宿主快照/导出取用面）。
     */
    @Bean
    public io.github.chyuan_cuihongyuan.buzhou.core.prompt.PromptUsageStats buzhouPromptUsageStats() {
        return new io.github.chyuan_cuihongyuan.buzhou.core.prompt.PromptUsageStats();
    }

    /**
     * spec 401 / T694：提示词注册表（Langfuse 借鉴——版本+标签双轴）。bean 恒在
     * （未配置 = 空注册表零行为变化）；{@code buzhou.prompt.templates} 播种——
     * 同 name 同 body 幂等跳过（重启不掀版本，诚实边界：note 不参与幂等口径）；
     * 声明 label 自动指向该名当前最新。spec 424：usage-tracking.enabled=true
     * 时包 UsageTrackingPromptRegistry（resolve 记账——默认关原样返回）。
     */
    @Bean
    public io.github.chyuan_cuihongyuan.buzhou.core.prompt.PromptRegistry buzhouPromptRegistry(
            BuzhouPromptProperties properties,
            BuzhouPromptUsageProperties usageProperties,
            io.github.chyuan_cuihongyuan.buzhou.core.prompt.PromptUsageStats usageStats) {
        io.github.chyuan_cuihongyuan.buzhou.core.prompt.PromptRegistry registry =
                new io.github.chyuan_cuihongyuan.buzhou.core.prompt.InMemoryPromptRegistry();
        for (BuzhouPromptProperties.TemplateSpec t : properties.templates()) {
            if (t.name() == null || t.name().isBlank() || t.body() == null) {
                continue; // 播种条目不完整跳过（yml 手误不阻断启动）
            }
            var existing = registry.resolve(t.name());
            if (existing.isEmpty() || !existing.get().body().equals(t.body())) {
                registry.publish(t.name(), t.body(), "yml-seed");
            }
            if (t.label() != null && !t.label().isBlank()) {
                registry.resolve(t.name())
                        .ifPresent(latest -> registry.label(t.name(), t.label(), latest.version()));
            }
        }
        return Boolean.TRUE.equals(usageProperties.enabled())
                ? new io.github.chyuan_cuihongyuan.buzhou.core.prompt.UsageTrackingPromptRegistry(
                        registry, usageStats)
                : registry;
    }

    /**
     * spec 330 / T652：告警通知策略门装配（{@code buzhou.alert.silences[]} 或
     * {@code buzhou.alert.inhibit-rules[]} 声明即装配；两键全空 = 不建门，引擎
     * 通知路径零变化）。yml 声明窗的 until = 装配时刻 + duration；机制引用在
     * 引擎 start() 期统一校验（与规则同口径 fail-fast）。
     */
    @Bean
    @org.springframework.context.annotation.Conditional(
            BuzhouCoreAutoConfiguration.AlertGatePresentCondition.class)
    public io.github.chyuan_cuihongyuan.buzhou.core.health.AlertGate buzhouAlertGate(
            BuzhouAlertProperties properties) {
        java.time.Instant now = java.time.Instant.now();
        java.util.List<io.github.chyuan_cuihongyuan.buzhou.core.health.AlertGate.Silence> silences =
                new java.util.ArrayList<>();
        java.util.List<BuzhouAlertProperties.SilenceSpec> specs = properties.silences();
        for (int i = 0; i < specs.size(); i++) {
            BuzhouAlertProperties.SilenceSpec spec = specs.get(i);
            silences.add(io.github.chyuan_cuihongyuan.buzhou.core.health.AlertGate.ymlSilence(
                    "yml-" + (i + 1), new java.util.LinkedHashSet<>(spec.mechanisms()),
                    spec.duration(), spec.comment(), spec.createdBy(), now));
        }
        java.util.List<io.github.chyuan_cuihongyuan.buzhou.core.health.AlertGate.InhibitRule> inhibits =
                properties.inhibitRules().stream()
                        .map(r -> new io.github.chyuan_cuihongyuan.buzhou.core.health
                                .AlertGate.InhibitRule(r.sourceMechanism(), r.targetMechanism()))
                        .toList();
        return new io.github.chyuan_cuihongyuan.buzhou.core.health.AlertGate(inhibits, silences, null);
    }

    /** spec 330：silences 或 inhibit-rules 非空才建门（Binder 预绑判定）。 */
    static final class AlertGatePresentCondition implements org.springframework.context.annotation.Condition {
        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext context,
                org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            try {
                org.springframework.boot.context.properties.bind.Binder binder =
                        org.springframework.boot.context.properties.bind.Binder.get(context.getEnvironment());
                boolean silences = !binder.bind("buzhou.alert.silences",
                        org.springframework.boot.context.properties.bind.Bindable
                                .listOf(BuzhouAlertProperties.SilenceSpec.class))
                        .orElse(java.util.List.of()).isEmpty();
                boolean inhibits = !binder.bind("buzhou.alert.inhibit-rules",
                        org.springframework.boot.context.properties.bind.Bindable
                                .listOf(BuzhouAlertProperties.InhibitSpec.class))
                        .orElse(java.util.List.of()).isEmpty();
                return silences || inhibits;
            } catch (RuntimeException e) {
                return false; // 绑定失败交由属性校验层报错
            }
        }
    }

    /** spec 312：rules 非空才装配（Binder 预绑判定——列表条件注解表达不了）。 */
    static final class AlertRulesPresentCondition implements org.springframework.context.annotation.Condition {
        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext context,
                org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            try {
                return !org.springframework.boot.context.properties.bind.Binder
                        .get(context.getEnvironment())
                        .bind("buzhou.alert.rules",
                                org.springframework.boot.context.properties.bind.Bindable
                                        .listOf(BuzhouAlertProperties.RuleSpec.class))
                        .orElse(java.util.List.of()).isEmpty();
            } catch (RuntimeException e) {
                return false; // 绑定失败交由属性校验层报错
            }
        }
    }

    /**
     * spec 307 / T605：事件 schema yml 声明装配（{@code buzhou.webhook.schema.required-keys.<type>}
     * + fail-open）——包装 forwarder 的 checker（缺键 fail-closed 丢弃 / 违规计数）。
     * 空声明返回 null（NullBean——类型收集自动跳过，零变化）。
     */
    @Bean
    public io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener buzhouEventSchemaChecker(
            io.github.chyuan_cuihongyuan.buzhou.core.webhook.BuzhouWebhookProperties props,
            org.springframework.beans.factory.ObjectProvider<
                    io.github.chyuan_cuihongyuan.buzhou.core.webhook.WebhookEventForwarder> forwarderProvider) {
        io.github.chyuan_cuihongyuan.buzhou.core.webhook.WebhookEventForwarder forwarder =
                forwarderProvider.getIfAvailable();
        if (forwarder == null) {
            return null; // 无投递面（未配 url）——契约无从谈起
        }
        io.github.chyuan_cuihongyuan.buzhou.core.webhook.BuzhouWebhookProperties.Schema schema =
                props.schema();
        if (schema == null || !schema.declared()) {
            return null; // NullBean：未声明契约 = 不拦截（零变化）
        }
        return new io.github.chyuan_cuihongyuan.buzhou.core.webhook.EventSchemaChecker(
                forwarder, schema.requiredKeySets(), schema.failOpen());
    }

    /**
     * spec 307 / T605：全局监听挂点去重——被 EventSchemaChecker 包装的 delegate
     * 不再直挂（防同一事件双投：一次经 checker 过滤、一次裸投）。
     */
    public static java.util.List<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener>
    effectiveGlobalListeners(
            java.util.List<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener> listeners) {
        java.util.Set<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener> wrapped =
                new java.util.HashSet<>();
        for (io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener listener : listeners) {
            if (listener instanceof io.github.chyuan_cuihongyuan.buzhou.core.webhook.EventSchemaChecker checker) {
                wrapped.add(checker.delegate());
            }
        }
        if (wrapped.isEmpty()) {
            return listeners;
        }
        return listeners.stream()
                .filter(listener -> !wrapped.contains(listener))
                .toList();
    }

    /**
     * spec 305 / T601：工具健康探测装配（{@code buzhou.tools.health.enabled=true}，Consul
     * health check 装配收尾）——ToolHealthProber bean + 周期自调度（interval 可配默认 30s），
     * 探针注册归宿主（框架不知道怎么探——分层诚实）；状态翻转计数；容器关闭停调度。
     */
    @Bean(destroyMethod = "stop")
    @ConditionalOnProperty(prefix = "buzhou.tools.health", name = "enabled", havingValue = "true")
    public io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolHealthProber buzhouToolHealthProber(
            BuzhouToolsProperties properties) {
        io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolHealthProber prober =
                new io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolHealthProber();
        prober.onChange((tool, status) ->
                io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                        .counter("buzhou.tools.health.flipped", 1,
                                "tool", tool, "to", status.status().name()));
        prober.start(properties.health().interval());
        return prober;
    }

    /** spec 305 / T601：探测健康面（严格口径：外部工具 DOWN 不拉低机制整体，详情显形 down 列表）。 */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(
            io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolHealthProber.class)
    public io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth buzhouToolHealth(
            io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolHealthProber prober) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolHealth(prober);
    }

    /**
     * spec 306 / T603：工具熔断 yml 装配（{@code buzhou.tools.circuit.enabled=true}，resilience4j
     * ——spec 131/165 原语装配面，fog 227「新 hook 配置面族」首项）。BuzhouHook bean
     * 由 {@code List<BuzhouHook>} 自动收集进 RuntimeConfig；默认关 = 零行为变化。
     */
    @Bean
    @ConditionalOnProperty(prefix = "buzhou.tools.circuit", name = "enabled", havingValue = "true")
    public io.github.chyuan_cuihongyuan.buzhou.core.concurrent.ToolCircuitBreakerHook buzhouToolCircuitBreakerHook(
            BuzhouToolsProperties properties) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.concurrent.ToolCircuitBreakerHook(
                new io.github.chyuan_cuihongyuan.buzhou.core.concurrent.ToolCircuitBreaker(
                        properties.circuit().toConfig(), java.time.Clock.systemUTC()));
    }

    /**
     * spec 302 / T596：进程级重试预算装配——{@code buzhou.backpressure.retry-budget} 任一键
     * 配置即启用（percent/min-balance，组内默认见 {@link BuzhouBackpressureProperties.RetryBudgetParams}），
     * 设定 {@code RetryBudgetHolder} 供模型重试（ResilienceAdvisor）与工具重试
     * （RetryingToolCallback）动态读取；未配置 = holder 保持 null（零行为变化）。
     * 容器关闭清 holder（防 ApplicationContextRunner 跨上下文静态残留）。
     */
    @Bean
    public org.springframework.beans.factory.DisposableBean buzhouRetryBudgetAdapter(
            BuzhouBackpressureProperties backpressureProperties) {
        BuzhouBackpressureProperties.RetryBudgetParams params = backpressureProperties.retryBudget();
        io.github.chyuan_cuihongyuan.buzhou.core.backpressure.RetryBudget budget = params == null ? null
                : io.github.chyuan_cuihongyuan.buzhou.core.backpressure.RetryBudget
                        .of(params.percent(), params.minBalance());
        io.github.chyuan_cuihongyuan.buzhou.core.backpressure.RetryBudgetHolder.set(budget);
        return () -> io.github.chyuan_cuihongyuan.buzhou.core.backpressure.RetryBudgetHolder.set(null);
    }

    /**
     * 事件外发 webhook（spec 20 / T89；outbox 持久化 spec 24 / T103 / impl-78）：配置
     * {@code buzhou.webhook.url} 才装配（默认关、零开销）。事件经持久化 outbox 投递
     * （stateStore 合成会话，重启恢复）；forwarder 经全局监听挂点挂全部会话（见 buzhouAgentRuntime）。
     */
    /** spec 39 §C / T140：outbox 水位健康面（forwarder 装配时随之）。 */
    @Bean
    @ConditionalOnProperty(prefix = "buzhou.webhook", name = "url")
    public io.github.chyuan_cuihongyuan.buzhou.core.webhook.WebhookOutboxHealth webhookOutboxHealth(
            io.github.chyuan_cuihongyuan.buzhou.core.webhook.WebhookEventForwarder forwarder) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.webhook.WebhookOutboxHealth(forwarder);
    }

    /** spec 39 §C / T140：索引装配态健康面（SessionIndexStore bean 存在时）。 */
    @Bean
    @ConditionalOnBean(io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore.class)
    public io.github.chyuan_cuihongyuan.buzhou.core.internal.session.SessionIndexHealth sessionIndexHealth(
            io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore indexStore) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.internal.session.SessionIndexHealth(indexStore);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "buzhou.webhook", name = "url")
    public io.github.chyuan_cuihongyuan.buzhou.core.webhook.WebhookEventForwarder webhookEventForwarder(
            io.github.chyuan_cuihongyuan.buzhou.core.webhook.BuzhouWebhookProperties props,
            org.springframework.beans.factory.ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores> storesProvider,
            org.springframework.core.env.Environment env) {
        io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores stores = storesProvider.getIfAvailable();
        io.github.chyuan_cuihongyuan.buzhou.core.webhook.WebhookEventForwarder forwarder =
                new io.github.chyuan_cuihongyuan.buzhou.core.webhook.WebhookEventForwarder(props,
                        stores != null ? stores.sessionStateStore()
                                : new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore());
        // spec 105 §A / T387：订阅类型过滤（buzhou.webhook.include-types——空/缺省 = 全投递）
        java.util.List<String> include = org.springframework.boot.context.properties.bind.Binder
                .get(env).bind("buzhou.webhook.include-types",
                        org.springframework.boot.context.properties.bind.Bindable.listOf(String.class))
                .orElse(java.util.List.of());
        forwarder.setIncludeTypes(include);
        // spec 533 / T817：载荷大小上限（0 = 不限——默认零变化；Kafka max message size 思想）
        Integer maxPayloadChars = env.getProperty("buzhou.webhook.max-payload-chars", Integer.class);
        if (maxPayloadChars != null && maxPayloadChars > 0) {
            forwarder.setOutboxMaxPayloadChars(maxPayloadChars);
        }
        // spec 728 / T1007：投递限速（buzhou.webhook.rate-limit-per-second>0 声明即启用；
        // burst 缺省 = ceil(rate)；envoy local rate limit 思想）
        Double ratePerSecond = env.getProperty("buzhou.webhook.rate-limit-per-second", Double.class);
        if (ratePerSecond != null && ratePerSecond > 0) {
            Integer burst = env.getProperty("buzhou.webhook.rate-limit-burst", Integer.class,
                    (int) Math.ceil(ratePerSecond));
            forwarder.setRateLimiter(new io.github.chyuan_cuihongyuan.buzhou.core.webhook
                    .WebhookRateLimiter(burst, ratePerSecond, System::currentTimeMillis));
        }
        return forwarder;
    }

    /**
     * 工具结果限幅器全局默认（spec 31 / T110 / impl-85）：启动期据配置设定 Holder；
     * 会话装配时 toolManager 从 Holder 取初值（可经 toolManager() per-session 覆盖）。
     */
    @Bean
    public io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolResultLimiter buzhouToolResultLimiter(
            BuzhouToolsProperties props) {
        java.util.Map<String, Integer> overrides = new java.util.LinkedHashMap<>();
        overrides.put("read_range", -1); // spill 自治理豁免（默认档）
        if (props.resultLimitOverrides() != null) {
            overrides.putAll(props.resultLimitOverrides());
        }
        io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolResultLimiter limiter =
                new io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolResultLimiter(
                        props.resultLimitChars(), overrides);
        io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolResultLimiterHolder.set(limiter);
        return limiter;
    }

    /**
     * 工具入参限幅器全局默认（spec 506 / T763——31 结果限幅的入站对称面）：
     * 默认 -1 不限（零默认行为变化——显式 opt-in）；Holder 模式同结果限幅。
     */
    @Bean
    public io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolInputLimiter buzhouToolInputLimiter(
            BuzhouToolsProperties props) {
        io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolInputLimiter limiter =
                new io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolInputLimiter(
                        props.inputLimitChars(), props.inputLimitOverrides());
        io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolInputLimiterHolder.set(limiter);
        return limiter;
    }

    // ---- 失控检测与容量闸（impl-45 / spec 14 §A，自分支增量移植）----

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "buzhou.runaway", name = "enabled", matchIfMissing = true)
    public io.github.chyuan_cuihongyuan.buzhou.core.runaway.RunawayCounters runawayCounters() {
        return new io.github.chyuan_cuihongyuan.buzhou.core.runaway.RunawayCounters();
    }

    /**
     * 失控检测 Hook（{@code buzhou.runaway.enabled} 默认开；阈值默认 null = 不限，
     * safe-by-default）。注入 {@link BuzhouStores} 的 observabilityStore 使
     * {@code runaway.*} 事件双重写入（SessionEvent + EventRecord），dashboard 可查。
     * store 经 {@code ObjectProvider} 惰性取用——store.type 校验失败路径上无 store bean 时
     * 不抢跑（该路径由 buzhouStoreTypeGuard 以 BuzhouConfigurationException 失败）。
     */
    @Bean
    @ConditionalOnMissingBean(name = "runawayHook")
    @ConditionalOnProperty(prefix = "buzhou.runaway", name = "enabled", matchIfMissing = true)
    public io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook runawayHook(
            BuzhouRunawayProperties runawayProperties,
            ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.runaway.RunawayCounters> counters,
            ObjectProvider<BuzhouStores> stores) {
        BuzhouStores available = stores.getIfAvailable();
        return new io.github.chyuan_cuihongyuan.buzhou.core.runaway.RunawayHook(
                runawayProperties, counters.getIfAvailable(
                        io.github.chyuan_cuihongyuan.buzhou.core.runaway.RunawayCounters::new),
                available == null ? null : available.observabilityStore());
    }

    /**
     * Token/成本预算 Hook（spec 16 / T83 / impl-58；{@code buzhou.token-budget.enabled} 默认开、
     * 阈值 null = 不限，safe-by-default）。模型名回退键取 {@code buzhou.model-name}（默认 unknown，
     * 与 resilience/observability 同口径）；{@code budget.*} 事件双写（SessionEvent + EventRecord）。
     */
    @Bean
    @ConditionalOnMissingBean(name = "tokenBudgetHook")
    @ConditionalOnProperty(prefix = "buzhou.token-budget", name = "enabled", matchIfMissing = true)
    public BuzhouHook tokenBudgetHook(
            BuzhouTokenBudgetProperties tokenBudgetProperties,
            ObjectProvider<BuzhouStores> stores,
            org.springframework.core.env.Environment env,
            ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.budget.VirtualKeys> virtualKeys) {
        BuzhouStores available = stores.getIfAvailable();
        // spec 158 / T511：active-key 配置时接 key 级预算闸（省缺 = 既有零变化）
        String activeKey = env.getProperty("buzhou.virtual-keys.active-key");
        io.github.chyuan_cuihongyuan.buzhou.core.budget.VirtualKeys keys = activeKey == null
                ? null : virtualKeys.getIfAvailable();
        return new io.github.chyuan_cuihongyuan.buzhou.core.budget.TokenBudgetHook(
                tokenBudgetProperties, env.getProperty("buzhou.model-name", "unknown"),
                available == null ? null : available.observabilityStore(), keys, activeKey,
                buzhouPricingTable(tokenBudgetProperties, env));
    }

    /**
     * spec 417 / T725：可变价目表（320/340 rebind 同模式 + Stripe 即时生效
     * 思想）——底表取 token-budget.pricing；BuzhouConfigRefreshEvent 整表
     * 热载覆盖层 + 逐键 WARN diff + 计数。bean 恒在（空表零行为）。
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(
            io.github.chyuan_cuihongyuan.buzhou.core.budget.PricingTable.class)
    public io.github.chyuan_cuihongyuan.buzhou.core.budget.PricingTable buzhouPricingTable(
            BuzhouTokenBudgetProperties properties,
            org.springframework.core.env.Environment environment) {
        return io.github.chyuan_cuihongyuan.buzhou.core.budget.PricingTable.of(properties, environment);
    }

    /**
     * spec 158 / T511：虚拟 key 注册表（active-key 配置时装配；limits 空表 +
     * active-key 组合在装配期 fail-fast——不带病上线）。健康段经
     * {@code @ConditionalOnBean(VirtualKeys)} 随之出现。
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            prefix = "buzhou.virtual-keys", name = "active-key")
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
    public io.github.chyuan_cuihongyuan.buzhou.core.budget.VirtualKeys buzhouVirtualKeys(
            BuzhouVirtualKeyProperties props,
            org.springframework.beans.factory.ObjectProvider<
                    io.github.chyuan_cuihongyuan.buzhou.core.spi.VirtualKeyBudgetBackend> sharedBackend) {
        if (props.limits() == null || props.limits().isEmpty()) {
            throw new BuzhouConfigurationException(
                    "buzhou.virtual-keys.active-key 配置了但 limits 为空——key 闸无从扣减",
                    "请补 buzhou.virtual-keys.limits.<key>=<token 硬顶>，或删除 active-key");
        }
        if (!props.limits().containsKey(props.activeKey())) {
            throw new BuzhouConfigurationException(
                    "buzhou.virtual-keys.active-key=[" + props.activeKey()
                            + "] 不在 limits 表里——省缺 key 的扣减是静默直通（诚实边界反被误用）",
                    "请在 limits 里给它设硬顶，或改 active-key");
        }
        // spec 315 / T621：共享后端在场（store.type=redis）→ 计数面跨实例共享；
        // 无 bean = 进程内计数（默认零变化）
        io.github.chyuan_cuihongyuan.buzhou.core.spi.VirtualKeyBudgetBackend backend =
                sharedBackend.getIfAvailable();
        io.github.chyuan_cuihongyuan.buzhou.core.budget.VirtualKeys keys = backend == null
                ? io.github.chyuan_cuihongyuan.buzhou.core.budget.VirtualKeys.create()
                : io.github.chyuan_cuihongyuan.buzhou.core.budget.VirtualKeys.withBackend(backend);
        props.limits().forEach(keys::register);
        return keys;
    }

    /**
     * 软退出提醒渲染器（达软阈值时经既有 Attachment 通道注入「剩余步数预算」信号）。
     * 被 {@code BuzhouMemoryAutoConfiguration} 自动组合进 CompositeAttachmentRenderer。
     * 无 {@code per-turn.max-steps} 时不注入（合法长任务不受影响）。
     */
    @Bean
    @ConditionalOnMissingBean(name = "runawayBudgetRenderer")
    @ConditionalOnProperty(prefix = "buzhou.runaway", name = "enabled", matchIfMissing = true)
    public io.github.chyuan_cuihongyuan.buzhou.core.spi.AttachmentRenderer runawayBudgetRenderer(
            BuzhouRunawayProperties runawayProperties,
            ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.runaway.RunawayCounters> counters) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.runaway.RunawayBudgetRenderer(
                runawayProperties, counters.getIfAvailable(
                        io.github.chyuan_cuihongyuan.buzhou.core.runaway.RunawayCounters::new));
    }

    /**
     * spec 42 §B / T156 / impl-127：读降级策略初始化——bean 创建即把全局默认写入
     * {@link io.github.chyuan_cuihongyuan.buzhou.core.spi.ReadDegradeHolder}（memory/jdbc/redis
     * 任何 store 形态都生效）；策略只认 off|empty，非法值在属性构造期 fail-fast。
     */
    @Bean
    public io.github.chyuan_cuihongyuan.buzhou.core.spi.ReadDegradePolicy buzhouReadDegradePolicy(
            BuzhouCoreProperties properties) {
        io.github.chyuan_cuihongyuan.buzhou.core.spi.ReadDegradePolicy policy =
                properties.store().readDegradePolicy();
        io.github.chyuan_cuihongyuan.buzhou.core.spi.ReadDegradeHolder.set(policy);
        return policy;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "buzhou.store", name = "type", havingValue = "memory", matchIfMissing = true)
    public BuzhouStores buzhouStores(BuzhouCoreProperties properties) {
        // impl-36 / spec 13 §growth-8：buzhou.store.in-memory.* 容量配额流入内存套件
        return Buzhou.inMemoryStores(properties.store().inMemory().toConfig());
    }

    /**
     * spec 48 §A / T174：turn 反馈导出扩展——自动并入 SessionExport.extensions
     * （与 memory.facts 同通道；宿主自定义同名 bean 可覆盖）。仅当 store bean 存在时装配
     * （store.type 拼错等 fail-fast 场景不抢跑、保持既有引导文案）。
     */
    @Bean
    @ConditionalOnBean(BuzhouStores.class)
    @ConditionalOnMissingBean(io.github.chyuan_cuihongyuan.buzhou.core.session.FeedbackExporter.class)
    public io.github.chyuan_cuihongyuan.buzhou.core.session.FeedbackExporter buzhouFeedbackExporter(
            BuzhouStores stores) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.session.FeedbackExporter(
                stores.sessionStateStore());
    }

    /**
     * impl-42 / spec 13 §T68：{@code buzhou.store.type} 封闭枚举 fail-fast——拼错值
     * （如 {@code jbdc}）此前会静默落进「无任何 store 装配」的深水区运行时失败；现在
     * 启动即失败并给出可选值与已装模块指引（经 {@link BuzhouStoreFailureAnalyzer} 翻译）。
     * 依赖顺序：本 bean 须在无 store 可用时不抢跑——用 {@code @ConditionalOnMissingBean}
     * + 显式类型校验双管（memory 默认路径自证；jdbc/redis 模块在场时由其 store bean 存在）。
     */
    @Bean
    public Object buzhouStoreTypeGuard(org.springframework.core.env.Environment env,
            ObjectProvider<BuzhouStores> stores) {
        String type = env.getProperty("buzhou.store.type", "memory").trim().toLowerCase();
        if (!java.util.Set.of("memory", "jdbc", "redis").contains(type)) {
            throw new BuzhouConfigurationException(
                    "buzhou.store.type=\"" + type + "\" 不是有效存储形态",
                    "修正为 memory（默认，进程内）/ jdbc（MySQL/PostgreSQL/H2，需 buzhou-store-jdbc）"
                            + "/ redis（需 buzhou-store-redis）之一；注意大小写与拼写",
                    null);
        }
        if (stores.getIfAvailable() == null) {
            throw new BuzhouConfigurationException(
                    "buzhou.store.type=" + type + " 但对应 store 实现未装配",
                    type.equals("jdbc") ? "引入 buzhou-store-jdbc 依赖（并确认 DataSource bean 存在）"
                            : type.equals("redis") ? "引入 buzhou-store-redis 依赖（并确认 uri 配置）"
                            : "检查 buzhou.store.* 配置",
                    null);
        }
        return new Object();
    }

    /**
     * impl-41 / spec 13 §T66：泄漏检测器（{@code buzhou.leak.level}=
     * DISABLED|SIMPLE|ADVANCED|PARANOID，默认 SIMPLE 1/128 采样；
     * {@code buzhou.leak.lease-age-threshold} 默认 PT5M；LeakListener bean 可注入）。
     * 安装即全局生效（会话/租约/spill 句柄三挂点经 LeakDetectorHolder 取用）。
     */
    @Bean
    @ConditionalOnMissingBean
    public io.github.chyuan_cuihongyuan.buzhou.core.leak.ResourceLeakDetector resourceLeakDetector(
            org.springframework.core.env.Environment env,
            ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.leak.ResourceLeakDetector.LeakListener> listener) {
        String levelText = env.getProperty("buzhou.leak.level",
                io.github.chyuan_cuihongyuan.buzhou.core.leak.ResourceLeakDetector.LeakLevel.SIMPLE.name());
        io.github.chyuan_cuihongyuan.buzhou.core.leak.ResourceLeakDetector.LeakLevel level;
        try {
            level = io.github.chyuan_cuihongyuan.buzhou.core.leak.ResourceLeakDetector.LeakLevel
                    .valueOf(levelText.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("buzhou.leak.level 非法：\"" + levelText
                    + "\"（须 DISABLED/SIMPLE/ADVANCED/PARANOID）", e);
        }
        // T214 勘察纠偏：metadata 宣称默认 "5m"，此处原 Duration.parse 只认 ISO "PT5M"——
        // 按文档配置启动即炸。改 Spring 双格式解析（"5m"/"PT5M" 均可）。
        java.time.Duration threshold;
        String thresholdText = env.getProperty("buzhou.leak.lease-age-threshold", "5m");
        try {
            threshold = org.springframework.boot.convert.DurationStyle.detectAndParse(thresholdText.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("buzhou.leak.lease-age-threshold 非法：\"" + thresholdText
                    + "\"（支持 5m/PT5M 两种格式，须为正时长）", e);
        }
        io.github.chyuan_cuihongyuan.buzhou.core.leak.ResourceLeakDetector detector =
                new io.github.chyuan_cuihongyuan.buzhou.core.leak.ResourceLeakDetector(
                        level, threshold, listener.getIfAvailable());
        io.github.chyuan_cuihongyuan.buzhou.core.leak.LeakDetectorHolder.install(detector);
        return detector;
    }

    /**
     * impl-41 / spec 13 §T66：有 micrometer（MeterRegistry bean）时——安装
     * MicrometerBuzhouMetrics 到全局 holder + 预注册标准指标集。未装 micrometer：
     * holder 保持 no-op（零开销）。
     */
    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @org.springframework.boot.autoconfigure.condition.ConditionalOnClass(
            io.micrometer.core.instrument.MeterRegistry.class)
    static class BuzhouMetricsConfiguration {

        @Bean
        @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(
                io.micrometer.core.instrument.MeterRegistry.class)
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsBinder buzhouMetricsBinder(
                io.micrometer.core.instrument.MeterRegistry registry) {
            return new io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsBinder();
        }

        @Bean
        @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(
                io.micrometer.core.instrument.MeterRegistry.class)
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolderInstaller
                buzhouMetricsHolderInstaller(io.micrometer.core.instrument.MeterRegistry registry,
                                             org.springframework.core.env.Environment env) {
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetrics metrics =
                    new io.github.chyuan_cuihongyuan.buzhou.core.metrics.MicrometerBuzhouMetrics(
                            registry);
            // spec 160 / T513：tag 基数守卫 opt-in（默认关零变化；开 = 装饰后安装——
            // per-(名,键) 值集封顶越限折 __overflow__，Loki cardinality limit 借鉴）
            if (env.getProperty("buzhou.metrics.cardinality-guard.enabled", Boolean.class,
                    Boolean.FALSE)) {
                metrics = io.github.chyuan_cuihongyuan.buzhou.core.metrics.TagCardinalityGuard
                        .wrap(metrics);
            }
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.install(metrics);
            return new io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolderInstaller();
        }
    }

    /**
     * impl-41 / spec 13 §T66：只读快照端点 {@code /actuator/buzhou}（有 actuator 才装配；
     * 聚合全部 BuzhouHealth 机制贡献）。
     */
    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @org.springframework.boot.autoconfigure.condition.ConditionalOnClass(
            name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
    static class BuzhouEndpointConfiguration {

        @Bean
        @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
        io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealthEndpoint buzhouHealthEndpoint(
                ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth> contributors) {
            return new io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealthEndpoint(
                    contributors.orderedStream().toList());
        }

        /**
         * spec 332 / T656：探针归类 + 裁决端点（K8s probes——liveness 失败→重启 /
         * readiness 失败→摘流量（缺省归类）/ startup 失败→等待）。点名机制在端点
         * 装配期 fail-fast 校验（机制集已知——晚于全部健康 bean 创建）。
         */
        @Bean
        @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
        io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouProbes buzhouProbes(
                BuzhouProbeProperties properties) {
            return new io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouProbes(
                    properties.livenessMechanisms(), properties.startupMechanisms());
        }

        @Bean
        @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
        io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouProbesEndpoint buzhouProbesEndpoint(
                ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth> contributors,
                io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouProbes probes) {
            java.util.List<io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth> list =
                    contributors.orderedStream().toList();
            probes.validateMechanisms(list.stream()
                    .map(io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth::mechanism)
                    .toList());
            return new io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouProbesEndpoint(
                    list, probes);
        }

        /** spec 343 / T678：生效配置自描述端点（密钥掩码宁掩勿漏）。 */
        @Bean
        @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
        io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouConfigSnapshotEndpoint
        buzhouConfigSnapshotEndpoint(org.springframework.core.env.Environment environment) {
            return new io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouConfigSnapshotEndpoint(
                    environment);
        }

        /** spec 345 / T682：告警面板端点（312 引擎 + 330 门状态聚合；缺席段空）。 */
        @Bean
        @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
        io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouAlertsEndpoint buzhouAlertsEndpoint(
                ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.health.AlertRuleEngine> engine,
                ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.health.AlertGate> gate) {
            return new io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouAlertsEndpoint(
                    engine.getIfAvailable(), gate.getIfAvailable());
        }

        /** spec 346 / T684：会话面板端点（活跃计数+地板多源+cordon——面板三部曲之三）。 */
        @Bean
        @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
        io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouSessionsEndpoint buzhouSessionsEndpoint(
                ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore> index,
                io.github.chyuan_cuihongyuan.buzhou.core.backpressure.SpawnAdmissionFloor floor,
                io.github.chyuan_cuihongyuan.buzhou.core.backpressure.MaintenanceCordon cordon,
                ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores> stores) {
            io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores bstores =
                    stores.getIfAvailable();
            return new io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouSessionsEndpoint(
                    index.getIfAvailable(), floor, cordon, 16,
                    bstores == null ? null : bstores.sessionStateStore());
        }

        /** spec 85 §A / T325：错误签名健康段（top-5 族 + 在册数；恒 UP——观测面）。 */
        @Bean
        @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
        io.github.chyuan_cuihongyuan.buzhou.core.health.ErrorSignaturesHealth buzhouErrorSignaturesHealth() {
            return new io.github.chyuan_cuihongyuan.buzhou.core.health.ErrorSignaturesHealth(
                    io.github.chyuan_cuihongyuan.buzhou.core.metrics.ErrorSignatures.global());
        }

        /**
         * spec 647 / T944：hook 计时进程级聚合 + 健康段（Holder 开启镜像——
         * 未装配零变化；HookTimingHealth 恒 UP，details = per-hook 计时）。
         */
        @Bean
        @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
        io.github.chyuan_cuihongyuan.buzhou.core.hook.HookTimingHealth buzhouHookTimingHealth() {
            io.github.chyuan_cuihongyuan.buzhou.core.hook.HookTimingAggregator.Holder.enable();
            return new io.github.chyuan_cuihongyuan.buzhou.core.hook.HookTimingHealth(
                    io.github.chyuan_cuihongyuan.buzhou.core.hook.HookTimingAggregator.Holder.current());
        }

        /**
         * spec 700 / T951：工具执行耗时进程级聚合 + 健康段（Holder 开启镜像——
         * 未装配零变化；ToolTimingHealth 恒 UP，details = per-tool 耗时/失败计数）。
         */
        @Bean
        @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
        io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolTimingHealth buzhouToolTimingHealth() {
            io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolTimingAggregator.Holder.enable();
            return new io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolTimingHealth(
                    io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolTimingAggregator.Holder.current());
        }

        /** spec 92 §A / T349：隔离舱健康段（未配置 UNKNOWN；配置后 per-agent 详情）。 */
        @Bean
        @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
        io.github.chyuan_cuihongyuan.buzhou.core.health.BulkheadHealth buzhouBulkheadHealth() {
            return new io.github.chyuan_cuihongyuan.buzhou.core.health.BulkheadHealth(
                    io.github.chyuan_cuihongyuan.buzhou.core.concurrent.AgentBulkhead.global());
        }

        /** spec 102 §A / T379：会话归档健康段（stores 缺席 = UNKNOWN-disabled，不抢 store 校验报错优先级）。 */
        @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(
            io.github.chyuan_cuihongyuan.buzhou.core.budget.VirtualKeys.class)
    public io.github.chyuan_cuihongyuan.buzhou.core.health.VirtualKeysHealth buzhouVirtualKeysHealth(
            io.github.chyuan_cuihongyuan.buzhou.core.budget.VirtualKeys keys) {
        // spec 154 / T507：宿主声明 VirtualKeys bean 时健康段自动出现（编程面默认无）
        return new io.github.chyuan_cuihongyuan.buzhou.core.health.VirtualKeysHealth(keys);
    }

    /** spec 192 / T553：模型成本健康段（台账全局恒在 + 预算钩子自动入账——恒有段）。 */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
    public io.github.chyuan_cuihongyuan.buzhou.core.health.ModelCostHealth buzhouModelCostHealth() {
        return new io.github.chyuan_cuihongyuan.buzhou.core.health.ModelCostHealth();
    }

    @Bean
        @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
        io.github.chyuan_cuihongyuan.buzhou.core.health.ArchiveHealth buzhouArchiveHealth(
                org.springframework.beans.factory.ObjectProvider<BuzhouStores> stores) {
            BuzhouStores available = stores.getIfAvailable();
            return new io.github.chyuan_cuihongyuan.buzhou.core.health.ArchiveHealth(
                    available == null ? null : available.sessionStateStore());
        }
    }

    /**
     * impl-30 / spec 13 §core-1：显式 {@code destroyMethod = "close"}——不靠推断，且与
     * {@link AgentRuntimeLifecycle#stop} 的双触发由 runtime 停机状态机（幂等）吸收：
     * stop 已跑完则 close 为 no-op；容器未调 stop 直接 destroy 时 close 兜底硬截断收尾。
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnBean(ChatModel.class)
    @ConditionalOnMissingBean(AgentRuntime.class)
    public DefaultAgentRuntime buzhouAgentRuntime(ChatModel chatModel, BuzhouStores stores,
                                           BuzhouCoreProperties properties,
                                           BuzhouBackpressureProperties backpressureProperties,
                                           List<RuntimeConfig> moduleConfigs,
                                           List<BuzhouHook> hooks,
                                           List<ToolCallback> autoTools,
                                           List<SessionAssemblyCustomizer> assemblyCustomizers,
                                           List<SessionResourceCustomizer> resourceCustomizers,
                                           ObjectProvider<MemoryViewProcessor> viewProcessor,
                                           ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener>
                                                   globalEventListeners,
                                           org.springframework.beans.factory.ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore>
                                                   indexStoreProvider,
                                           org.springframework.beans.factory.ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionExportExtension>
                                                   exportExtensionsProvider,
                                           org.springframework.beans.factory.ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.backpressure.SpawnAdmissionFloor>
                                                   spawnAdmissionFloorProvider,
                                           org.springframework.beans.factory.ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolBaggage>
                                                   toolBaggageProvider) {
        List<RuntimeConfig> all = new ArrayList<>(moduleConfigs);
        // 用户自定义扩展 bean（按组件类型包成单维度 RC 后并入 merge；模块产出已在 moduleConfigs 内）
        if (!hooks.isEmpty()) {
            all.add(RuntimeConfig.hooks(hooks));
        }
        if (!autoTools.isEmpty()) {
            all.add(RuntimeConfig.autoTools(autoTools));
        }
        if (!assemblyCustomizers.isEmpty()) {
            all.add(RuntimeConfig.assemblyCustomizers(assemblyCustomizers));
        }
        // spec 30 / T109 / impl-84：会话索引接线（store 模块提供 SessionIndexStore bean 才启用；
        // 无 bean = 无枚举能力，会话功能零影响——最终一致索引）
        io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore indexStore =
                indexStoreProvider.getIfAvailable();
        if (indexStore != null) {
            // spec 37 §C / T134：保留期（buzhou.index.closed-retention；默认 30d，-1/0 永久）
            io.github.chyuan_cuihongyuan.buzhou.core.internal.session.SessionIndexObserver
                    .configureRetention(properties.core().indexClosedRetention());
            all.add(RuntimeConfig.assemblyCustomizers(java.util.List.of(
                    io.github.chyuan_cuihongyuan.buzhou.core.internal.session.SessionIndexObserver
                            .wiring(indexStore))));
            // spec 33 §B / T113：会话删除级联置 DELETED（审计留存；物理删由运维按保留策略）
            all.add(RuntimeConfig.cleanupContributors(java.util.List.of(
                    io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleanupContributor.of(
                            "session-index",
                            sessionId -> indexStore.get(sessionId).ifPresent(info ->
                                    indexStore.upsert(new io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo(
                                            info.sessionId(), info.appId(), info.agentName(),
                                            io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo.STATUS_DELETED,
                                            info.createdAtEpochMs(), info.lastActiveAtEpochMs(),
                                            info.turnCount(), info.tags())))))));
        }
        if (!resourceCustomizers.isEmpty()) {
            all.add(RuntimeConfig.sessionCustomizers(resourceCustomizers));
        }
        MemoryViewProcessor mvp = viewProcessor.getIfAvailable();
        if (mvp != null) {
            all.add(RuntimeConfig.viewProcessor(mvp));
        }
        RuntimeConfig merged = RuntimeConfig.merge(all.toArray(new RuntimeConfig[0]));
        // impl-33 / spec 13 §core-3：租约参数（buzhou.lease-ttl / buzhou.lease-renew-interval）流入运行时；
        // impl-30：停机排空预算（buzhou.lifecycle.timeout-per-shutdown-phase）流入运行时；
        // impl-34 / spec 13 §core-4：事件分发模式（buzhou.core.event-dispatch.*）流入运行时
        io.github.chyuan_cuihongyuan.buzhou.core.session.EventDispatchConfig eventDispatch =
                properties.core().eventDispatch().toConfig();
        // impl-45 / spec 14 §A：spawn 容量闸（buzhou.backpressure.max-concurrent-sessions 配置且
        // 机制启用时构建；未配置 / 关闭 = null 不限，既有行为不变）
        BuzhouBackpressureProperties bp = backpressureProperties;
        io.github.chyuan_cuihongyuan.buzhou.core.backpressure.SpawnAdmissionFloor freezeFloor =
                spawnAdmissionFloorProvider.getIfAvailable(); // spec 335：政策驱动、gate 读取
        io.github.chyuan_cuihongyuan.buzhou.core.backpressure.SpawnGate spawnGate =
                bp != null && bp.enabled() && bp.maxConcurrentSessions() != null
                        && bp.maxConcurrentSessions() > 0
                        ? new io.github.chyuan_cuihongyuan.buzhou.core.backpressure.SpawnGate(
                                bp.maxConcurrentSessions(), bp.effectiveSpawnQueueTimeout(),
                                bp.effectiveSpawnOverloadPolicy(), event -> {
                                }, freezeFloor)
                        : null;
        DefaultAgentRuntime runtime = new DefaultAgentRuntime(chatModel, stores,
                new HarnessAssembler().withToolTimeout(properties.core().toolTimeout())
                        // spec 46 §B / T171：流累计上限（buzhou.core.stream-total-timeout；
                        // 属性层已归一：正值生效 / ZERO 显式关闭 / 未配默认 10m）
                        .withStreamTotalTimeout(properties.core().streamTotalTimeout())
                        // spec 337 / T666：工具上下文行李（bean 恒在——运行时 API 必须预先在场）
                        .withToolBaggage(toolBaggageProvider.getIfAvailable()), merged,
                properties.leaseTtl(), properties.effectiveLeaseRenewInterval(),
                properties.lifecycle().timeoutPerShutdownPhase(),
                eventDispatch.isBuffered() ? eventDispatch : null,
                spawnGate);
        // spec 20 / T89 / impl-64：全局事件监听 bean（如 WebhookEventForwarder）挂全部会话；
        // spec 307 / T605：schema checker 在场时其 delegate 去重（防双投）
        effectiveGlobalListeners(globalEventListeners.stream().toList())
                .forEach(runtime::addGlobalEventListener);
        // spec 36 §A / T121：导出扩展 bean（模块自有段进 SessionExport.extensions）
        runtime.setExportExtensions(exportExtensionsProvider.orderedStream().toList());
        return runtime;
    }

    /**
     * impl-30 / spec 13 §core-1：core 优雅停机 lifecycle（phase =
     * {@link BuzhouLifecyclePhases#CORE}，最先 stop——拒绝新 Turn → 在途 AFTER_CURRENT_TURN
     * 取消 → 排空 → 超时硬截断）。
     */
    @Bean
    @ConditionalOnBean(ChatModel.class)
    public AgentRuntimeLifecycle buzhouAgentRuntimeLifecycle(DefaultAgentRuntime runtime) {
        return new AgentRuntimeLifecycle(runtime, null);
    }

    /**
     * spec 84 §A / T323：agent 并发 Turn 隔离舱（opt-in {@code buzhou.bulkhead.enabled=true}
     * + {@code buzhou.bulkhead.agents.<agent>=<maxConcurrentTurns>}，默认关——全 NOOP 零行为；
     * resilience4j Bulkhead 思想：spawn 闸限会话数，本舱限在飞 Turn 数，正交）。未配
     * agents 表时开启也是 NOOP（诚实：无上限可执行）。
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            prefix = "buzhou.bulkhead", name = "enabled", havingValue = "true")
    public io.github.chyuan_cuihongyuan.buzhou.core.concurrent.AgentBulkhead buzhouAgentBulkhead(
            org.springframework.core.env.Environment env) {
        java.util.Map<String, Integer> limits = org.springframework.boot.context.properties.bind.Binder
                .get(env).bind("buzhou.bulkhead.agents",
                        org.springframework.boot.context.properties.bind.Bindable.mapOf(
                                String.class, Integer.class))
                .orElse(java.util.Map.of());
        java.time.Duration timeout = org.springframework.boot.context.properties.bind.Binder
                .get(env).bind("buzhou.bulkhead.acquire-timeout",
                        org.springframework.boot.context.properties.bind.Bindable
                                .of(java.time.Duration.class))
                .orElse(java.time.Duration.ZERO);
        io.github.chyuan_cuihongyuan.buzhou.core.concurrent.AgentBulkhead bulkhead =
                io.github.chyuan_cuihongyuan.buzhou.core.concurrent.AgentBulkhead.of(limits, timeout);
        io.github.chyuan_cuihongyuan.buzhou.core.concurrent.AgentBulkhead.install(bulkhead);
        return bulkhead;
    }

    /**
     * spec 320 / T632：舱容量热重载监听（舱开即装配）：宿主改完
     * {@code buzhou.bulkhead.agents} 后发布 {@link BuzhouConfigRefreshEvent}
     * ——重读 yml → resize 全局舱，容量热生效不重启（Spring Cloud rebind 思想，
     * 事件自持不引依赖）。
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            prefix = "buzhou.bulkhead", name = "enabled", havingValue = "true")
    public io.github.chyuan_cuihongyuan.buzhou.core.concurrent.BulkheadHotReload
    buzhouBulkheadHotReload(
            io.github.chyuan_cuihongyuan.buzhou.core.concurrent.AgentBulkhead bulkhead,
            org.springframework.core.env.Environment env) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.concurrent.BulkheadHotReload(
                bulkhead, env);
    }

    /**
     * spec 319 / T630：舱压伸缩建议装配（{@code buzhou.bulkhead.scaling.scale-up-threshold}
     * 配置且舱开启才装配——K8s HPA 思想：窗口拒绝增量 → 实例倍率建议，只建议不执行）。
     * 舱未开（NOOP 舱拒绝恒 0，建议恒 1）不装配；复合条件 Binder 预绑判定
     * （312 同法——条件注解表达不了「另一开关 + 本键」）。
     */
    @Bean
    @org.springframework.context.annotation.Conditional(
            BuzhouCoreAutoConfiguration.BulkheadScalingCondition.class)
    public io.github.chyuan_cuihongyuan.buzhou.core.concurrent.BulkheadScalingAdvisor
    buzhouBulkheadScalingAdvisor(
            BulkheadScalingProperties properties,
            io.github.chyuan_cuihongyuan.buzhou.core.concurrent.AgentBulkhead bulkhead) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.concurrent.BulkheadScalingAdvisor(
                bulkhead, properties.scaleUpThreshold(),
                properties.maxMultiplier() == null
                        ? BulkheadScalingProperties.DEFAULT_MAX_MULTIPLIER
                        : properties.maxMultiplier());
    }

    /** spec 319：threshold 配置且 {@code buzhou.bulkhead.enabled=true} 才装配。 */
    static final class BulkheadScalingCondition implements org.springframework.context.annotation.Condition {
        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext context,
                org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            boolean bulkheadEnabled = org.springframework.boot.context.properties.bind.Binder
                    .get(context.getEnvironment())
                    .bind("buzhou.bulkhead.enabled", Boolean.class).orElse(false);
            if (!bulkheadEnabled) {
                return false;
            }
            return org.springframework.boot.context.properties.bind.Binder
                    .get(context.getEnvironment())
                    .bind("buzhou.bulkhead.scaling.scale-up-threshold", Long.class)
                    .isBound();
        }
    }

    /**
     * spec 91 §A / T345 + spec 107 §A / T391：配置体检（opt-in
     * {@code buzhou.config-doctor.enabled=true}，默认关；Spring Shell doctor 思想）——
     * 就绪事件跑一次 ConfigDoctor：发现走日志 + 报告缓存进健康段（/actuator/buzhou
     * 的 config-doctor 段：errors/warnings/checkedKeys）。只读不写——报告不改行为。
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            prefix = "buzhou.config-doctor", name = "enabled", havingValue = "true")
    public io.github.chyuan_cuihongyuan.buzhou.core.health.ConfigDoctorHealth buzhouConfigDoctorHealth(
            org.springframework.core.env.Environment env) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.health.ConfigDoctorHealth(env);
    }

    /**
     * spec 69 §A / T290：崩溃自愈 watchdog（opt-in {@code buzhou.recovery.auto-resume=true}，
     * 默认关；Temporal crash-watchdog 思想）——启动完成后枚举 RUNNING 快照逐一续跑
     * （steal=false：他方活跃实例持锁即跳过，仅接管疑似崩溃者）。无 RunRegistry bean
     * 或未开启时零操作。SmartLifecycle start 一次（幂等；续跑历史加载触发悬空修复 +
     * 事件日志回放——与手工 restart 同语义）。
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            prefix = "buzhou.recovery", name = "auto-resume", havingValue = "true")
    public org.springframework.context.SmartLifecycle buzhouCrashResumeWatchdog(
            org.springframework.beans.factory.ObjectProvider<
                    io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunRegistry> runRegistry,
            DefaultAgentRuntime runtime) {
        io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunRegistry registry =
                runRegistry.getIfAvailable();
        return new org.springframework.context.SmartLifecycle() {
            private boolean running;

            @Override
            public void start() {
                running = true;
                if (registry != null) {
                    new io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunRecoveryService(
                            registry, runtime).autoResumeAll();
                }
            }

            @Override
            public void stop() {
                running = false;
            }

            @Override
            public boolean isRunning() {
                return running;
            }
        };
    }

    /**
     * impl-37 / spec 13 §stores-6：保留策略族后台执行器（{@code buzhou.retention.*}）。
     * bean 恒在（{@code enabled=false} 只关自启动调度——各策略仍可手动
     * {@code RetentionSweeper#sweepOnce()} 触发）。恢复设施（ToolCallLog/RunRegistry）
     * 作为 bean 声明时并入级联清理与窗口批删；未声明时仅五槽 store 参与保留兑现。
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public io.github.chyuan_cuihongyuan.buzhou.core.retention.RetentionSweeper buzhouRetentionSweeper(
            BuzhouStores stores,
            BuzhouRetentionProperties retention,
            org.springframework.beans.factory.ObjectProvider<
                    io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLog> toolCallLog,
            org.springframework.beans.factory.ObjectProvider<
                    io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunRegistry> runRegistry,
            org.springframework.beans.factory.ObjectProvider<
                    io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaderElector> leaderElector) {
        io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLog tcl = toolCallLog.getIfAvailable();
        io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunRegistry registry = runRegistry.getIfAvailable();
        return new io.github.chyuan_cuihongyuan.buzhou.core.retention.RetentionSweeper(
                new io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleaner(stores, registry, tcl),
                stores.observabilityStore(),
                stores.summaryStore(),
                tcl,
                registry,
                new io.github.chyuan_cuihongyuan.buzhou.core.retention.SessionHistoryPolicy(
                        retention.sessionRetention(), retention.sessionNotBefore()),
                new io.github.chyuan_cuihongyuan.buzhou.core.retention.ObservabilityTtl(
                        retention.observabilityTtl(), retention.observabilityBatchSize()),
                retention.summaryKeepVersions(),
                retention.toolCallLogRetention(),
                retention.runCompletedRetention(),
                new io.github.chyuan_cuihongyuan.buzhou.core.retention.MaintenanceTrigger(
                        retention.trigger().base(), retention.trigger().scaleFactor(),
                        retention.trigger().cap(), retention.trigger().hardFloor()),
                retention.sweepInterval(),
                null,
                retention.enabled(),
                leaderElector.getIfAvailable()); // spec 331：无 bean = 无门零变化
    }

    /**
     * spec 127 / T455：会话归档器 bean（宿主未自建时兜底——spec 97 归档冷层的
     * 自动装配面；SessionCleaner 与 sweeper 各持实例，装配参数同源）。
     */
    @Bean
    @ConditionalOnMissingBean
    public io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionArchiver buzhouSessionArchiver(
            BuzhouStores stores,
            org.springframework.core.env.Environment environment,
            org.springframework.beans.factory.ObjectProvider<
                    io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore> indexStore,
            org.springframework.beans.factory.ObjectProvider<
                    io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLog> toolCallLog,
            org.springframework.beans.factory.ObjectProvider<
                    io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunRegistry> runRegistry) {
        // spec 726 / T1003：归档 PDB 闸（buzhou.cleanup.min-available-sessions>0 且
        // 索引在场才装配——capped probe 计数：limit=min+1 一页，size>min 即放行）
        int minAvailable = environment.getProperty(
                "buzhou.cleanup.min-available-sessions", Integer.class, 0);
        io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionAvailabilityFloor floor = null;
        var index = indexStore.getIfAvailable();
        if (minAvailable > 0 && index != null) {
            floor = new io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionAvailabilityFloor(
                    minAvailable, () -> index.list(
                            new io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexQuery(
                                    null, null, null, null, null, 0, minAvailable + 1, null))
                            .size());
        }
        return new io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionArchiver(
                stores,
                new io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleaner(
                        stores, runRegistry.getIfAvailable(), toolCallLog.getIfAvailable()),
                floor);
    }

    /**
     * spec 127 / T455：归档 TTL 定时清理（{@code buzhou.session-archive.purge-enabled}
     * 默认关——删除动作必须显式开启；开启后单线程 scheduleWithFixedDelay 兑现
     * purgeTtl；多实例各跑一份，幂等无害）。
     */
    /**
     * spec 538 / T827：store fsck 定时巡检（341 选主扩散第三弹——对账面从
     * 手工触发升级定时巡检）。{@code buzhou.fsck.enabled=true} 装配；只读
     * 巡检不自动修复（repair 仍归手工面）；elector 缺席 = 无门单实例跑。
     */
    @Bean
    @org.springframework.context.annotation.Conditional(
            BuzhouCoreAutoConfiguration.FsckPresentCondition.class)
    public io.github.chyuan_cuihongyuan.buzhou.core.cleanup.StoreFsckHousekeeper
    buzhouStoreFsckHousekeeper(
            BuzhouStores stores,
            BuzhouFsckProperties fsck,
            ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaderElector> leaderElector) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.cleanup.StoreFsckHousekeeper(
                stores, leaderElector.getIfAvailable(), fsck.interval());
    }

    /** spec 538：enabled=true 才装配（Binder 预绑判定——426 同法）。 */
    static final class FsckPresentCondition
            implements org.springframework.context.annotation.Condition {
        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext context,
                org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            try {
                return org.springframework.boot.context.properties.bind.Binder
                        .get(context.getEnvironment())
                        .bind("buzhou.fsck.enabled", Boolean.class)
                        .map(Boolean::booleanValue).orElse(false);
            } catch (Exception e) {
                return false;
            }
        }
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "buzhou.session-archive", name = "purge-enabled",
            havingValue = "true")
    public io.github.chyuan_cuihongyuan.buzhou.core.retention.ArchivePurgeJob buzhouArchivePurgeJob(
            io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionArchiver archiver,
            BuzhouArchiveProperties archive,
            ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaderElector> leaderElector) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.retention.ArchivePurgeJob(
                archiver, archive.getPurgeTtl(), archive.getPurgeInterval(),
                archive.isPurgeEnabled(), null,
                leaderElector.getIfAvailable()); // spec 341：无 bean = 零变化
    }
}
