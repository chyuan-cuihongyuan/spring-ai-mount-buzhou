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
@EnableConfigurationProperties(ResilienceProperties.class)
public class BuzhouResilienceAutoConfiguration {

    @Bean
    public ResilienceStats resilienceStats() {
        return new ResilienceStats();
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
        return new io.github.chyuan_cuihongyuan.buzhou.resilience.shadow.ShadowComparisonJsonl(
                java.nio.file.Path.of(properties.shadow().detailPath()));
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
