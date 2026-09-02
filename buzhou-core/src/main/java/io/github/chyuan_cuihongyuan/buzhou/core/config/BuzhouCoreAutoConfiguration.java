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
        SessionDisruptionBudgetProperties.class, BulkheadScalingProperties.class})
public class BuzhouCoreAutoConfiguration {

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
                    io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth> healthBeans) {
        java.util.List<io.github.chyuan_cuihongyuan.buzhou.core.health.AlertRuleEngine.AlertRule> rules =
                properties.rules().stream()
                        .map(r -> new io.github.chyuan_cuihongyuan.buzhou.core.health
                                .AlertRuleEngine.AlertRule(r.name(), r.mechanism(), r.forDuration()))
                        .toList();
        return new io.github.chyuan_cuihongyuan.buzhou.core.health.AlertRuleEngine(rules,
                () -> {
                    java.util.Map<String, io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth> map =
                            new java.util.LinkedHashMap<>();
                    healthBeans.orderedStream()
                            .forEach(health -> map.put(health.mechanism(), health));
                    return map;
                }, properties.interval());
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
                available == null ? null : available.observabilityStore(), keys, activeKey);
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

        /** spec 85 §A / T325：错误签名健康段（top-5 族 + 在册数；恒 UP——观测面）。 */
        @Bean
        @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
        io.github.chyuan_cuihongyuan.buzhou.core.health.ErrorSignaturesHealth buzhouErrorSignaturesHealth() {
            return new io.github.chyuan_cuihongyuan.buzhou.core.health.ErrorSignaturesHealth(
                    io.github.chyuan_cuihongyuan.buzhou.core.metrics.ErrorSignatures.global());
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
                                                   exportExtensionsProvider) {
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
        io.github.chyuan_cuihongyuan.buzhou.core.backpressure.SpawnGate spawnGate =
                bp != null && bp.enabled() && bp.maxConcurrentSessions() != null
                        && bp.maxConcurrentSessions() > 0
                        ? new io.github.chyuan_cuihongyuan.buzhou.core.backpressure.SpawnGate(
                                bp.maxConcurrentSessions(), bp.effectiveSpawnQueueTimeout(),
                                bp.effectiveSpawnOverloadPolicy(), event -> {
                                })
                        : null;
        DefaultAgentRuntime runtime = new DefaultAgentRuntime(chatModel, stores,
                new HarnessAssembler().withToolTimeout(properties.core().toolTimeout())
                        // spec 46 §B / T171：流累计上限（buzhou.core.stream-total-timeout；
                        // 属性层已归一：正值生效 / ZERO 显式关闭 / 未配默认 10m）
                        .withStreamTotalTimeout(properties.core().streamTotalTimeout()), merged,
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
                    io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunRegistry> runRegistry) {
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
                retention.enabled());
    }

    /**
     * spec 127 / T455：会话归档器 bean（宿主未自建时兜底——spec 97 归档冷层的
     * 自动装配面；SessionCleaner 与 sweeper 各持实例，装配参数同源）。
     */
    @Bean
    @ConditionalOnMissingBean
    public io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionArchiver buzhouSessionArchiver(
            BuzhouStores stores,
            org.springframework.beans.factory.ObjectProvider<
                    io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLog> toolCallLog,
            org.springframework.beans.factory.ObjectProvider<
                    io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunRegistry> runRegistry) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionArchiver(
                stores,
                new io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleaner(
                        stores, runRegistry.getIfAvailable(), toolCallLog.getIfAvailable()));
    }

    /**
     * spec 127 / T455：归档 TTL 定时清理（{@code buzhou.session-archive.purge-enabled}
     * 默认关——删除动作必须显式开启；开启后单线程 scheduleWithFixedDelay 兑现
     * purgeTtl；多实例各跑一份，幂等无害）。
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "buzhou.session-archive", name = "purge-enabled",
            havingValue = "true")
    public io.github.chyuan_cuihongyuan.buzhou.core.retention.ArchivePurgeJob buzhouArchivePurgeJob(
            io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionArchiver archiver,
            BuzhouArchiveProperties archive) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.retention.ArchivePurgeJob(
                archiver, archive.getPurgeTtl(), archive.getPurgeInterval(),
                archive.isPurgeEnabled());
    }
}
