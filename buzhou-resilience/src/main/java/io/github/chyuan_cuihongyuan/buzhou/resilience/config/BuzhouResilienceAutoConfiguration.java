package io.github.chyuan_cuihongyuan.buzhou.resilience.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.resilience.ResilienceModule;
import io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.NamedFallbackModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 模型韧性层自装配（spec 15 / 09；impl-44 补运维面、impl-57 补备模型降级链）。
 *
 * <p>safe-by-default：{@code buzhou.resilience.enabled} 未配置时默认开（引入即生效、可一键关）。
 * 暴露一个 {@link RuntimeConfig} bean，由 core 收集并 merge——其内装配定制器把
 * {@code ResilienceAdvisor} + {@code RateLimitAdvisor}（如配置了限流）注入 advisor 链。
 *
 * <p>模型名取 {@code buzhou.model-name}（默认 {@code unknown}，与 observability 模块同口径）。
 * 备模型降级链（impl-57）：{@code buzhou.resilience.fallback.models} 为 ChatModel bean 名列表，
 * 按名解析、未命中启动失败（fail-fast 防拼写错静默失效）。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "buzhou.resilience", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties({ResilienceProperties.class, BuzhouRoutingProperties.class,
        io.github.chyuan_cuihongyuan.buzhou.resilience.structured.StructuredOutputProperties.class,
        BuzhouModelConcurrencyProperties.class,
        io.github.chyuan_cuihongyuan.buzhou.resilience.capability.BuzhouModelCapabilityProperties.class,
        io.github.chyuan_cuihongyuan.buzhou.resilience.routing.BuzhouRoutingScheduleProperties.class,
        io.github.chyuan_cuihongyuan.buzhou.resilience.idempotency.BuzhouIdempotencyProperties.class})
public class BuzhouResilienceAutoConfiguration {

    @Bean
    public ResilienceStats resilienceStats() {
        return new ResilienceStats();
    }

    /**
     * spec 426 / T744 + spec 429 / T750：模型并发舱（Resilience4j
     * SemaphoreBulkhead / Uber concurrency-limits 借鉴——供应商并发配额
     * 分层）。{@code buzhou.resilience.model-concurrency.limits} 非空声明即
     * 装配；spec 429 拆三 bean——limiter 恒 exposed（advisor 与热更新共享
     * 同一实例），ModelConcurrencyHotReload 随 refresh 事件热调容
     * （320/340 rebind 同模式）。模型名取 {@code buzhou.model-name}
     * （默认 unknown，同口径）。多实例诚实边界：每实例独立并发额度。
     */
    @Bean
    @org.springframework.context.annotation.Conditional(
            BuzhouResilienceAutoConfiguration.ModelConcurrencyPresentCondition.class)
    public io.github.chyuan_cuihongyuan.buzhou.resilience.concurrency.ModelConcurrencyLimiter
    buzhouModelConcurrencyLimiter(BuzhouModelConcurrencyProperties properties) {
        return new io.github.chyuan_cuihongyuan.buzhou.resilience.concurrency.ModelConcurrencyLimiter(
                properties.limits(), properties.acquireTimeout());
    }

    @Bean
    @org.springframework.context.annotation.Conditional(
            BuzhouResilienceAutoConfiguration.ModelConcurrencyPresentCondition.class)
    public RuntimeConfig modelConcurrencyRuntimeConfig(
            io.github.chyuan_cuihongyuan.buzhou.resilience.concurrency.ModelConcurrencyLimiter limiter,
            org.springframework.core.env.Environment env) {
        String modelName = env.getProperty("buzhou.model-name", "unknown");
        return new RuntimeConfig(java.util.List.of(), java.util.Set.of(), java.util.Set.of(),
                null, java.util.List.of(), java.util.Map.of(), java.util.List.of(),
                java.util.List.of(ctx -> ctx.addAdvisor(
                        new io.github.chyuan_cuihongyuan.buzhou.resilience.concurrency
                                .ModelConcurrencyAdvisor(limiter, modelName))),
                null);
    }

    /** spec 429 / T750：refresh 事件热调容（limiter bean 共享实例）。 */
    @Bean
    @org.springframework.context.annotation.Conditional(
            BuzhouResilienceAutoConfiguration.ModelConcurrencyPresentCondition.class)
    public io.github.chyuan_cuihongyuan.buzhou.resilience.concurrency.ModelConcurrencyHotReload
    buzhouModelConcurrencyHotReload(
            io.github.chyuan_cuihongyuan.buzhou.resilience.concurrency.ModelConcurrencyLimiter limiter,
            org.springframework.core.env.Environment env) {
        return new io.github.chyuan_cuihongyuan.buzhou.resilience.concurrency
                .ModelConcurrencyHotReload(limiter, env);
    }

    /** spec 426：limits map 非空才装配（Binder 预绑判定——406 同法）。 */
    static final class ModelConcurrencyPresentCondition
            implements org.springframework.context.annotation.Condition {
        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext context,
                org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            try {
                return org.springframework.boot.context.properties.bind.Binder
                        .get(context.getEnvironment())
                        .bind("buzhou.resilience.model-concurrency.limits",
                                org.springframework.boot.context.properties.bind.Bindable
                                        .mapOf(String.class, Integer.class))
                        .map(m -> !m.isEmpty()).orElse(false);
            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * spec 501 / T753：请求幂等键（Stripe Idempotency-Key 借鉴——客户端超时
     * 重试同键重入重放首次终态响应，不二次真调二次计费）。存储复用
     * ResponseCacheStore（LRU+TTL+计数自带）；键缺席=透传零行为；
     * {@code buzhou.resilience.idempotency.enabled=true} 显式开启才装配。
     */
    @Bean
    @org.springframework.context.annotation.Conditional(
            BuzhouResilienceAutoConfiguration.IdempotencyPresentCondition.class)
    public io.github.chyuan_cuihongyuan.buzhou.resilience.cache.ResponseCacheStore
    buzhouIdempotencyStore(io.github.chyuan_cuihongyuan.buzhou.resilience.idempotency
            .BuzhouIdempotencyProperties properties) {
        return new io.github.chyuan_cuihongyuan.buzhou.resilience.cache.ResponseCacheStore(
                properties.maxEntries(), properties.ttl());
    }

    @Bean
    @org.springframework.context.annotation.Conditional(
            BuzhouResilienceAutoConfiguration.IdempotencyPresentCondition.class)
    public RuntimeConfig idempotencyRuntimeConfig(
            io.github.chyuan_cuihongyuan.buzhou.resilience.cache.ResponseCacheStore
                    buzhouIdempotencyStore) {
        return new RuntimeConfig(java.util.List.of(), java.util.Set.of(), java.util.Set.of(),
                null, java.util.List.of(), java.util.Map.of(), java.util.List.of(),
                java.util.List.of(ctx -> ctx.addAdvisor(
                        new io.github.chyuan_cuihongyuan.buzhou.resilience.idempotency
                                .IdempotencyAdvisor(buzhouIdempotencyStore))),
                null);
    }

    /** spec 501：enabled=true 才装配（Binder 预绑判定——426 同法）。 */
    static final class IdempotencyPresentCondition
            implements org.springframework.context.annotation.Condition {
        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext context,
                org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            try {
                return org.springframework.boot.context.properties.bind.Binder
                        .get(context.getEnvironment())
                        .bind("buzhou.resilience.idempotency.enabled", Boolean.class)
                        .map(Boolean::booleanValue).orElse(false);
            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * spec 502 / T755：模型能力注册表与能力门（LiteLLM Router capabilities
     * 借鉴——vision/工具请求事前拦，供应商 400 变结构化异常）。
     * {@code buzhou.resilience.model-capabilities.<model>} 非空声明才装配；
     * 未注册模型零门零行为（声明渐进）。
     */
    @Bean
    @org.springframework.context.annotation.Conditional(
            BuzhouResilienceAutoConfiguration.CapabilityPresentCondition.class)
    public io.github.chyuan_cuihongyuan.buzhou.resilience.capability.ModelCapabilityRegistry
    buzhouModelCapabilityRegistry(org.springframework.core.env.Environment env) {
        // spec 531 装配审计修复：单 Map 组件 record 构造绑定在 prefix.<组件名> 子路径，
        // 根前缀 yml 必须根绑定直读（原 properties 注入绑空——注册表静默空 → 门零裁决）
        java.util.Map<String, io.github.chyuan_cuihongyuan.buzhou.resilience.capability.ModelCapabilities>
                models = org.springframework.boot.context.properties.bind.Binder
                        .get(env)
                        .bind("buzhou.resilience.model-capabilities",
                                org.springframework.boot.context.properties.bind.Bindable.mapOf(
                                        String.class,
                                        io.github.chyuan_cuihongyuan.buzhou.resilience.capability.ModelCapabilities.class))
                        .orElse(java.util.Map.of());
        return new io.github.chyuan_cuihongyuan.buzhou.resilience.capability
                .ModelCapabilityRegistry(models);
    }

    @Bean
    @org.springframework.context.annotation.Conditional(
            BuzhouResilienceAutoConfiguration.CapabilityPresentCondition.class)
    public RuntimeConfig capabilityGateRuntimeConfig(
            io.github.chyuan_cuihongyuan.buzhou.resilience.capability.ModelCapabilityRegistry registry,
            org.springframework.core.env.Environment env) {
        String modelName = env.getProperty("buzhou.model-name", "unknown");
        return new RuntimeConfig(java.util.List.of(), java.util.Set.of(), java.util.Set.of(),
                null, java.util.List.of(), java.util.Map.of(), java.util.List.of(),
                java.util.List.of(ctx -> ctx.addAdvisor(
                        new io.github.chyuan_cuihongyuan.buzhou.resilience.capability
                                .CapabilityGateAdvisor(registry, modelName))),
                null);
    }

    /** spec 502：capabilities map 非空才装配（Binder 预绑判定——426 同法）。 */
    static final class CapabilityPresentCondition
            implements org.springframework.context.annotation.Condition {
        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext context,
                org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            try {
                return org.springframework.boot.context.properties.bind.Binder
                        .get(context.getEnvironment())
                        .bind("buzhou.resilience.model-capabilities",
                                org.springframework.boot.context.properties.bind.Bindable.mapOf(
                                        String.class, io.github.chyuan_cuihongyuan.buzhou
                                                .resilience.capability.ModelCapabilities.class))
                        .map(m -> !m.isEmpty()).orElse(false);
            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * spec 503 / T757：时段路由窗口（K8s CronJob / Argo Rollouts schedule
     * 思想——时间窗驱动权重自动切换，夜间切便宜模型白天回切零人工值守）。
     * windows 非空才装配；直依赖 WeightedChatModel（路由未配而窗已声明 =
     * 跨键矛盾，启动红即诚实——doctor 115 同口径）。
     */
    @Bean
    @org.springframework.context.annotation.Conditional(
            BuzhouResilienceAutoConfiguration.RoutingSchedulePresentCondition.class)
    public io.github.chyuan_cuihongyuan.buzhou.resilience.routing.RoutingScheduleAdjuster
    routingScheduleAdjuster(
            io.github.chyuan_cuihongyuan.buzhou.resilience.routing.WeightedChatModel chatModel,
            BuzhouRoutingProperties routing,
            io.github.chyuan_cuihongyuan.buzhou.resilience.routing.BuzhouRoutingScheduleProperties
                    schedule) {
        return new io.github.chyuan_cuihongyuan.buzhou.resilience.routing.RoutingScheduleAdjuster(
                chatModel, schedule.windows(), routing.weights(), schedule.checkInterval(), null);
    }

    /** spec 503：windows 非空才装配（Binder 预绑判定——426 同法）。 */
    static final class RoutingSchedulePresentCondition
            implements org.springframework.context.annotation.Condition {
        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext context,
                org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            try {
                return org.springframework.boot.context.properties.bind.Binder
                        .get(context.getEnvironment())
                        .bind("buzhou.routing.schedule.windows",
                                org.springframework.boot.context.properties.bind.Bindable
                                        .listOf(io.github.chyuan_cuihongyuan.buzhou.resilience
                                                .routing.BuzhouRoutingScheduleProperties
                                                .RoutingWindow.class))
                        .map(w -> !w.isEmpty()).orElse(false);
            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * spec 339 / T670：多模型加权路由（LiteLLM Router 借鉴——199 平滑加权
     * 原语装配收尾）。{@code buzhou.routing.weights.<beanName>} ≥2 项才装配
     * @Primary 路由器（按名取 ChatModel bean，缺名启动红带修法——与 fallback
     * 同 fail-fast 口径）；未配/单项 = 零变化（条件不满足不建 bean）。
     */
    @Bean
    @org.springframework.context.annotation.Primary
    @org.springframework.context.annotation.Conditional(
            BuzhouResilienceAutoConfiguration.RoutingConfiguredCondition.class)
    public io.github.chyuan_cuihongyuan.buzhou.resilience.routing.WeightedChatModel
    buzhouWeightedChatModel(BuzhouRoutingProperties routing, Map<String, ChatModel> chatModels) {
        java.util.Map<String, ChatModel> candidates = new java.util.LinkedHashMap<>();
        routing.weights().keySet().forEach(name -> {
            ChatModel model = chatModels.get(name);
            if (model == null) {
                throw new BuzhouConfigurationException(
                        "buzhou.routing.weights 引用 ChatModel bean「" + name + "」不存在",
                        "可用 ChatModel bean：" + chatModels.keySet());
            }
            candidates.put(name, model);
        });
        return new io.github.chyuan_cuihongyuan.buzhou.resilience.routing.WeightedChatModel(
                candidates, routing.weights());
    }

    /**
     * spec 340 / T672：路由权重热重载（路由器在场即挂——320 舱容量同模式；
     * refresh 事件重读 yml 逐路 setWeight，WRR 动量保留自然收敛）。
     */
    @Bean
    @org.springframework.context.annotation.Conditional(
            BuzhouResilienceAutoConfiguration.RoutingConfiguredCondition.class)
    public io.github.chyuan_cuihongyuan.buzhou.resilience.routing.RoutingWeightsHotReload
    buzhouRoutingWeightsHotReload(
            io.github.chyuan_cuihongyuan.buzhou.resilience.routing.WeightedChatModel router,
            org.springframework.core.env.Environment environment) {
        return new io.github.chyuan_cuihongyuan.buzhou.resilience.routing.RoutingWeightsHotReload(
                router, environment);
    }

    /** spec 339：weights ≥2 路才建路由器（Binder 预绑判定——未配零变化）。 */
    static final class RoutingConfiguredCondition implements org.springframework.context.annotation.Condition {
        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext context,
                org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            try {
                return org.springframework.boot.context.properties.bind.Binder
                        .get(context.getEnvironment())
                        .bind("buzhou.routing.weights",
                                org.springframework.boot.context.properties.bind.Bindable
                                        .mapOf(String.class, Integer.class))
                        .orElse(java.util.Map.of()).size() >= 2;
            } catch (RuntimeException e) {
                return false; // 绑定失败交由属性校验层报错
            }
        }
    }

    @Bean
    public RuntimeConfig resilienceRuntimeConfig(ResilienceProperties properties, Environment env,
            ResilienceStats stats, Map<String, ChatModel> chatModels,
            org.springframework.beans.factory.ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.spi.RateLimitBackend> sharedBackend,
            org.springframework.beans.factory.ObjectProvider<org.springframework.ai.embedding.EmbeddingModel> embeddingModels,
            org.springframework.beans.factory.ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.spi.CircuitBreakerStateBackend> sharedCircuitBackend) {
        String modelName = env.getProperty("buzhou.model-name", "unknown");
        boolean sharedCircuit = sharedCircuitBackend.getIfAvailable() != null;
        warnIfMultiInstanceSemantics(properties, env, sharedBackend.getIfAvailable() != null, sharedCircuit);
        // spec 49 §A / T176：shadow 模型按名解析（与 fallback 同 fail-fast 口径）
        // spec 54 §A / T224：共享限流后端优先（store.type=redis 时 store-redis 供 bean）；
        // 无 bean = 内存令牌桶（默认零变化）
        // spec 55 §C / T242：语义缓存嵌入模型（semantic-cache.enabled 且无 bean → configure 内 fail-fast）
        // spec 57 §A / T255：共享熔断闸后端优先（store.type=redis 时 store-redis 供 bean）；
        // 无 bean = 进程语义（默认零变化）
        return ResilienceModule.configure(properties, modelName, stats,
                resolveFallbacks(properties, chatModels),
                resolveShadows(properties, chatModels),
                sharedBackend.getIfAvailable(),
                embeddingModels.getIfAvailable(),
                sharedCircuitBackend.getIfAvailable());
    }

    /**
     * spec 402 / T696：结构化输出执法（instructor 借鉴——验证失败错误喂回
     * 模型自修复）。独立 RuntimeConfig bean（assembly customizer 注 advisor，
     * 不动 ResilienceModule 内路）；{@code buzhou.resilience.structured-output.enabled=true}
     * 声明即装配。enabled 而 schema 全空 = 配置错误 fail-fast；声明类型不在
     * 支持集同样 fail-fast（拼写错不静默宽容）。
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            prefix = "buzhou.resilience.structured-output", name = "enabled", havingValue = "true")
    public RuntimeConfig structuredOutputRuntimeConfig(
            io.github.chyuan_cuihongyuan.buzhou.resilience.structured.StructuredOutputProperties props) {
        var spec = props.schema();
        if (spec == null || spec.isEmpty()) {
            throw new BuzhouConfigurationException(
                    "buzhou.resilience.structured-output.enabled=true 但 schema 全空",
                    "声明 schema.required 或 schema.properties（键→类型："
                            + io.github.chyuan_cuihongyuan.buzhou.resilience.structured.OutputSchema
                                    .knownTypes() + "）");
        }
        for (String type : spec.normalizedTypes().values()) {
            if (!io.github.chyuan_cuihongyuan.buzhou.resilience.structured.OutputSchema
                    .knownTypes().contains(type)) {
                throw new BuzhouConfigurationException(
                        "buzhou.resilience.structured-output.schema.properties 声明类型「"
                                + type + "」不在支持集",
                        "支持：" + io.github.chyuan_cuihongyuan.buzhou.resilience.structured.OutputSchema
                                .knownTypes());
            }
        }
        io.github.chyuan_cuihongyuan.buzhou.resilience.structured.OutputSchema schema =
                new io.github.chyuan_cuihongyuan.buzhou.resilience.structured.OutputSchema(
                        spec.required(), spec.normalizedTypes());
        int attempts = props.effectiveMaxRepairAttempts();
        return new RuntimeConfig(java.util.List.of(), java.util.Set.of(), java.util.Set.of(),
                null, java.util.List.of(), java.util.Map.of(), java.util.List.of(),
                java.util.List.of(ctx -> ctx.addAdvisor(
                        new io.github.chyuan_cuihongyuan.buzhou.resilience.structured
                                .StructuredOutputAdvisor(schema, attempts, ctx::emitEvent))),
                null);
    }

    /**
     * impl-74 / T99 / spec 23：多实例语义显式化——store.type=jbdc/redis 是多实例部署信号，
     * 而熔断/日配额是单进程机制（每实例独立额度）。启动 WARN 一次指向 runbook §6；
     * 不做配置拒绝（粘性路由 + 租约独占是合法部署形态，只是要知情）。
     * spec 54 §A / T224：限流在共享后端（Redis 固定窗）下跨实例共享额度——不再计入
     * 单进程告警；无共享后端时限流仍单进程（每实例独立额度，N 实例 = N 倍）。
     * spec 57 §A / T255：熔断在共享后端（Redis TTL 标记）下跳闸事实跨实例共享——
     * 不再计入单进程告警。
     */
    private static void warnIfMultiInstanceSemantics(ResilienceProperties properties, Environment env,
            boolean sharedRateLimitBackend, boolean sharedCircuitBackend) {
        String storeType = env.getProperty("buzhou.store.type", "memory");
        if ("memory".equals(storeType)) {
            return; // 单实例信号，无告警必要
        }
        boolean rateLimit = !sharedRateLimitBackend && properties.rateLimit() != null
                && (properties.rateLimit().requestsPerMinute() != null
                        || properties.rateLimit().tokensPerMinute() != null);
        boolean quota = properties.sessionQuota() != null
                && io.github.chyuan_cuihongyuan.buzhou.resilience.quota.SessionQuotaHook
                        .anyDimension(properties.sessionQuota());
        boolean circuit = !sharedCircuitBackend && properties.circuit() != null
                && properties.circuit().effectiveEnabled();
        if (rateLimit || quota || circuit) {
            System.getLogger(BuzhouResilienceAutoConfiguration.class.getName()).log(
                    System.Logger.Level.WARNING,
                    "检测到多实例部署信号（buzhou.store.type=" + storeType + "）且启用单进程机制"
                            + (rateLimit ? "（限流——无共享后端，每实例独立额度）" : "")
                            + (circuit ? "（熔断——无共享后端，每实例独立跳闸）" : "")
                            + "（日配额任一）：单进程机制 = N 实例独立额度/跳闸。"
                            + "推荐部署：粘性路由 + 租约独占（steal 接管）。"
                            + "限流/熔断跨实例共享见 store.type=redis 共享闸（spec 54/57 / runbook §6）；"
                            + "配额计数已原子扣减（spec 56），额度共享按会话粘性天然成立。");
        }
    }

    /** 按 bean 名解析备模型链：未命中名 fail-fast（拼写错不静默失效）。 */
    private static List<NamedFallbackModel> resolveFallbacks(ResilienceProperties properties,
            Map<String, ChatModel> chatModels) {
        ResilienceProperties.Fallback fallback = properties.fallback();
        if (fallback == null || !fallback.enabled()) {
            return null;
        }
        List<NamedFallbackModel> resolved = new ArrayList<>(fallback.models().size());
        for (String name : fallback.models()) {
            ChatModel model = chatModels.get(name);
            if (model == null) {
                throw new BuzhouConfigurationException(
                        "buzhou.resilience.fallback.models（" + name + "）未命中任何 ChatModel bean",
                        "检查 bean 名拼写；容器内可用 ChatModel bean：" + chatModels.keySet());
            }
            resolved.add(new NamedFallbackModel(name, model));
        }
        return resolved;
    }

    /**
     * spec 309 / T609：影子对照明细 JSONL 导出——{@code buzhou.resilience.shadow.detail-path}
     * 声明即装配（shadow.compared 事件逐条追加；SessionEventListener 类型由 core 全局
     * 挂点自动收集）。打开失败 fail-fast（坏路径该红）。
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "buzhou.resilience.shadow", name = "detail-path")
    public io.github.chyuan_cuihongyuan.buzhou.resilience.shadow.ShadowComparisonJsonl
    buzhouShadowComparisonJsonl(ResilienceProperties properties) throws java.io.IOException {
        // spec 643 / T936：轮转档位 yml 透传（缺省默认 64MB×3；显式 ≤0 = 关）
        return new io.github.chyuan_cuihongyuan.buzhou.resilience.shadow.ShadowComparisonJsonl(
                java.nio.file.Path.of(properties.shadow().detailPath()),
                properties.shadow().effectiveDetailMaxBytes(),
                properties.shadow().effectiveDetailMaxHistory());
    }

    /**
     * spec 301 / impl-324：对冲专用虚拟线程执行器（对冲竞速线程；随容器关闭 shutdown）。
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "buzhou.resilience.hedge", name = "enabled", havingValue = "true")
    public java.util.concurrent.ExecutorService buzhouHedgeExecutor() {
        return java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * spec 301 / impl-324：对冲装配（spec 137 原语 → 装配面）——{@code hedge.enabled=true}
     * 时注册 {@code @Primary} 的 {@code buzhouHedgedChatModel}：主模型超 delay 未回即并发
     * 押注对冲模型，先回先得。按名解析主/冲 bean（未命中 fail-fast 带可用名清单）；
     * 按类型取 ChatModel 的注入位（含 Spring AI ChatClient.Builder 装配）升为对冲装饰器，
     * 按名注入（fallback/shadow 解析）不受影响。诚实边界：开启后宿主不得再自标
     * {@code @Primary} ChatModel（对冲位即事实主位）。
     */
    @Bean
    @org.springframework.context.annotation.Primary
    @ConditionalOnProperty(prefix = "buzhou.resilience.hedge", name = "enabled", havingValue = "true")
    public ChatModel buzhouHedgedChatModel(ResilienceProperties properties,
            java.util.concurrent.ExecutorService buzhouHedgeExecutor,
            Map<String, ChatModel> chatModels) {
        ResilienceProperties.Hedge hedge = properties.hedge();
        ChatModel primary = chatModels.get(hedge.primaryModel());
        if (primary == null) {
            throw new BuzhouConfigurationException(
                    "buzhou.resilience.hedge.primary-model（" + hedge.primaryModel()
                            + "）未命中任何 ChatModel bean",
                    "检查 bean 名拼写；容器内可用 ChatModel bean：" + chatModels.keySet());
        }
        ChatModel hedgeModel = chatModels.get(hedge.model());
        if (hedgeModel == null) {
            throw new BuzhouConfigurationException(
                    "buzhou.resilience.hedge.model（" + hedge.model()
                            + "）未命中任何 ChatModel bean",
                    "检查 bean 名拼写；容器内可用 ChatModel bean：" + chatModels.keySet());
        }
        return new io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.HedgedChatModel(
                primary, hedgeModel, hedge.delay(), buzhouHedgeExecutor);
    }

    /** spec 49 §A / T176：按 bean 名解析 shadow 模型（未命中 fail-fast；未启用返回 null）。 */
    private static List<NamedFallbackModel> resolveShadows(ResilienceProperties properties,
            Map<String, ChatModel> chatModels) {
        ResilienceProperties.Shadow shadow = properties.shadow();
        if (shadow == null || !shadow.effectiveEnabled()) {
            return null;
        }
        List<NamedFallbackModel> resolved = new ArrayList<>(shadow.models().size());
        for (String name : shadow.models()) {
            ChatModel model = chatModels.get(name);
            if (model == null) {
                throw new BuzhouConfigurationException(
                        "buzhou.resilience.shadow.models（" + name + "）未命中任何 ChatModel bean",
                        "检查 bean 名拼写；容器内可用 ChatModel bean：" + chatModels.keySet());
            }
            resolved.add(new NamedFallbackModel(name, model));
        }
        return resolved;
    }
}
