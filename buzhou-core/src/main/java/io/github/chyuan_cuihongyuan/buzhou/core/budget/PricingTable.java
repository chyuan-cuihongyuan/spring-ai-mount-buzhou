package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouTokenBudgetProperties;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 可变价目表（spec 417 / T725，320/340 rebind 同模式 + Stripe 版本化价目
 * 即时生效思想）：静态底表（构造期 properties）+ volatile 热载覆盖层；
 * {@link #of} 覆盖层优先。热载 = 收 BuzhouConfigRefreshEvent 整表替换
 * （删除键语义靠整表表达——旧覆盖键消失即回落底表）+ WARN 逐键 diff +
 * 计数。底表不变（props record 仍是静态事实源）。
 */
public final class PricingTable implements ApplicationListener<io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigRefreshEvent> {

    /** 每百万 token 单价（USD）——与 TokenBudgetProperties.Pricing 同构。 */
    public record Price(BigDecimal inputPerMillion, BigDecimal outputPerMillion) {
    }

    private static final System.Logger LOGGER = System.getLogger(PricingTable.class.getName());

    private final Map<String, Price> base;
    private final Environment environment; // 可空——编程面无热载
    private volatile Map<String, Price> override = Map.of();
    private volatile long reloads;

    public PricingTable(Map<String, Price> base, Environment environment) {
        this.base = base == null ? Map.of() : Map.copyOf(base);
        this.environment = environment;
    }

    /** 从 properties 底表构建。 */
    public static PricingTable of(BuzhouTokenBudgetProperties props, Environment environment) {
        Map<String, Price> base = new LinkedHashMap<>();
        if (props != null && props.pricing() != null) {
            props.pricing().forEach((model, p) -> base.put(model,
                    new Price(p.inputPerMillion(), p.outputPerMillion())));
        }
        return new PricingTable(base, environment);
    }

    /** 查价：覆盖层优先；无价 null（计量 0 诚实）。 */
    public Price of(String model) {
        Price hot = override.get(model);
        return hot != null ? hot : base.get(model);
    }

    /** 热载次数（观测面）。 */
    public long reloads() {
        return reloads;
    }

    @Override
    public void onApplicationEvent(io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigRefreshEvent event) {
        if (environment == null) {
            return;
        }
        Map<String, BuzhouTokenBudgetProperties.Pricing> fresh = Binder.get(environment)
                .bind("buzhou.token-budget.pricing",
                        Bindable.mapOf(String.class, BuzhouTokenBudgetProperties.Pricing.class))
                .orElse(Map.of());
        Map<String, Price> next = new LinkedHashMap<>();
        fresh.forEach((model, p) -> {
            if (p != null) {
                next.put(model, new Price(p.inputPerMillion(), p.outputPerMillion()));
            }
        });
        diffWarn(next);
        this.override = Map.copyOf(next);
        this.reloads = reloads + 1;
        BuzhouMetricsHolder.metrics().counter("buzhou.pricing.reloaded");
    }

    /** 逐键 diff WARN（审计面——调价时刻与幅度可查）。 */
    private void diffWarn(Map<String, Price> next) {
        Map<String, Price> old = effective();
        for (Map.Entry<String, Price> e : next.entrySet()) {
            Price previous = old.get(e.getKey());
            if (previous == null || !previous.equals(e.getValue())) {
                LOGGER.log(System.Logger.Level.WARNING, "价目热载：{0} {1} -> {2}",
                        e.getKey(), previous == null ? "(无)" : previous, e.getValue());
            }
        }
        for (String gone : old.keySet()) {
            if (!next.containsKey(gone)) {
                LOGGER.log(System.Logger.Level.WARNING, "价目热载：{0} 移除（回落底表）", gone);
            }
        }
    }

    private Map<String, Price> effective() {
        Map<String, Price> merged = new LinkedHashMap<>(base);
        merged.putAll(override);
        return merged;
    }
}
