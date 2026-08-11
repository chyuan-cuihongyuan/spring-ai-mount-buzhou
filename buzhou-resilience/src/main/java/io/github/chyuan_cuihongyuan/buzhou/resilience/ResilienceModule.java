package io.github.chyuan_cuihongyuan.buzhou.resilience;

import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAssemblyContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAssemblyCustomizer;
import io.github.chyuan_cuihongyuan.buzhou.resilience.advisor.ModelCallInFlight;
import io.github.chyuan_cuihongyuan.buzhou.resilience.advisor.ResilienceAdvisor;
import io.github.chyuan_cuihongyuan.buzhou.resilience.advisor.ResilienceSessionObserver;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 模型韧性层模块入口（spec「模块与改动面」）。
 *
 * <p>独立可用、仅依赖 {@code buzhou-core}，遵守星形依赖白名单（不与 memory/spill/guard/tools/store-* 互依）。
 * {@link #configure(ResilienceProperties)} 返回一个只含「装配定制器」的 {@link RuntimeConfig}：
 * 定制器在会话装配期把 {@link ResilienceAdvisor} 注入 ChatClient advisor 链最内层。
 *
 * <p>装配形态对齐 {@code ObservabilityModule}（贡献 advisor 而非 hook 的模块）：
 * core 收集所有 {@link RuntimeConfig} bean 并 merge，故自装配侧只需暴露一个 {@code RuntimeConfig} bean。
 */
public final class ResilienceModule {

    private ResilienceModule() {
    }

    /**
     * @param properties 韧性参数（null 字段取规范默认）
     * @return 含一个装配定制器的 {@link RuntimeConfig}；{@code enabled=false} 时返回 {@link RuntimeConfig#defaults()}（不注入 advisor）
     */
    public static RuntimeConfig configure(ResilienceProperties properties) {
        if (!properties.enabled()) {
            return RuntimeConfig.defaults();
        }
        ProviderErrorClassifier classifier = new DefaultErrorClassifier();
        return RuntimeConfig.assemblyCustomizers(
                List.of(new ResilienceAssemblyCustomizer(properties, classifier)));
    }

    static final class ResilienceAssemblyCustomizer implements SessionAssemblyCustomizer {
        private final ResilienceProperties properties;
        private final ProviderErrorClassifier classifier;

        ResilienceAssemblyCustomizer(ResilienceProperties properties, ProviderErrorClassifier classifier) {
            this.properties = properties;
            this.classifier = classifier;
        }

        @Override
        public void customize(SessionAssemblyContext ctx) {
            // 虚拟线程执行器：deadline 兜底 + cancel 中断在途模型调用复用同一条路径。
            // 每会话一个，随会话关闭由 ResilienceSessionObserver.shutdownNow()。
            ExecutorService deadlineExecutor = Executors.newVirtualThreadPerTaskExecutor();
            ModelCallInFlight inFlight = new ModelCallInFlight();
            ResilienceAdvisor advisor = new ResilienceAdvisor(
                    properties, classifier, ctx::emitEvent, deadlineExecutor, inFlight);
            ctx.addAdvisor(advisor);
            // onCancel 中断在途模型调用（补 session.cancel() 漏网）；onClose 关执行器防泄漏。
            ctx.addObserver(new ResilienceSessionObserver(deadlineExecutor, inFlight));
        }
    }
}
