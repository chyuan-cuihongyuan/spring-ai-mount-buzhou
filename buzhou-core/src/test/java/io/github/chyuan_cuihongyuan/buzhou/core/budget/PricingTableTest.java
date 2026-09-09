package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigRefreshEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouTokenBudgetProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 417 §Testing / T725–T726：价目热更新——底表查询；覆盖优先；整表替换
 * 回落；hook 接线差异断言；bean 恒在。
 */
class PricingTableTest {

    private static BuzhouTokenBudgetProperties.Pricing price(String in, String out) {
        return new BuzhouTokenBudgetProperties.Pricing(new BigDecimal(in), new BigDecimal(out));
    }

    @Test
    void shouldPreferOverrideAndFallBackOnFullReplace() {
        Map<String, Object> yml = new HashMap<>();
        Map<String, BuzhouTokenBudgetProperties.Pricing> base = new HashMap<>();
        base.put("m1", price("1.0", "2.0"));
        BuzhouTokenBudgetProperties props = new BuzhouTokenBudgetProperties(
                null, null, null, null, base, null);
        StandardEnvironment env = new StandardEnvironment();
        PricingTable table = PricingTable.of(props, env);

        assertThat(table.of("m1")).isEqualTo(new PricingTable.Price(
                new BigDecimal("1.0"), new BigDecimal("2.0")));  // 底表
        assertThat(table.of("unknown")).isNull();                 // 无价 null

        // 热载：yml 覆盖 m1 新价 + 新增 m2
        yml.put("buzhou.token-budget.pricing.m1.input-per-million", "3.0");
        yml.put("buzhou.token-budget.pricing.m1.output-per-million", "4.0");
        yml.put("buzhou.token-budget.pricing.m2.input-per-million", "10.0");
        yml.put("buzhou.token-budget.pricing.m2.output-per-million", "20.0");
        env.getPropertySources().addFirst(new MapPropertySource("refresh", yml));
        table.onApplicationEvent(new BuzhouConfigRefreshEvent(this));
        assertThat(table.reloads()).isEqualTo(1);
        assertThat(table.of("m1")).isEqualTo(new PricingTable.Price(
                new BigDecimal("3.0"), new BigDecimal("4.0")));  // 覆盖优先
        assertThat(table.of("m2")).isNotNull();

        // 再热载整表替换：m1 覆盖消失 → 回落底表 1.0/2.0（删除键语义）
        Map<String, Object> yml2 = new HashMap<>();
        yml2.put("buzhou.token-budget.pricing.m2.input-per-million", "11.0");
        yml2.put("buzhou.token-budget.pricing.m2.output-per-million", "21.0");
        env.getPropertySources().replace("refresh", new MapPropertySource("refresh2", yml2));
        table.onApplicationEvent(new BuzhouConfigRefreshEvent(this));
        assertThat(table.of("m1")).isEqualTo(new PricingTable.Price(
                new BigDecimal("1.0"), new BigDecimal("2.0")));  // 回落底表
        assertThat(table.of("m2").inputPerMillion()).isEqualByComparingTo("11.0");
        assertThat(table.reloads()).isEqualTo(2);
    }

    @Test
    void shouldLetHookUseHotPriceImmediately() {
        Map<String, BuzhouTokenBudgetProperties.Pricing> base = new HashMap<>();
        base.put("m", price("1.0", "1.0"));
        BuzhouTokenBudgetProperties props = new BuzhouTokenBudgetProperties(
                null, null, null, null, base, null);
        StandardEnvironment env = new StandardEnvironment();
        PricingTable table = PricingTable.of(props, env);

        TokenBudgetHook hooked = new TokenBudgetHook(props, "m", null, null, null, table);
        TokenBudgetHook legacy = new TokenBudgetHook(props, "m", null);

        // 热载 10 倍价：hooked 即时翻 10 倍、legacy 仍底表（旧行为零变化）
        Map<String, Object> yml = new HashMap<>();
        yml.put("buzhou.token-budget.pricing.m.input-per-million", "10.0");
        yml.put("buzhou.token-budget.pricing.m.output-per-million", "10.0");
        env.getPropertySources().addFirst(new MapPropertySource("r", yml));
        table.onApplicationEvent(new BuzhouConfigRefreshEvent(this));

        long hot = hookedCost(hooked);
        long old = hookedCost(legacy);
        assertThat(old).isEqualTo(1_000_000L); // 1M token × $1/M = $1 = 1,000,000 microUsd
        assertThat(hot).isEqualTo(old * 10);
    }

    /** 经反射调私有 microUsd（1M prompt + 0 completion → microUsd）。 */
    private long hookedCost(TokenBudgetHook hook) {
        try {
            var m = TokenBudgetHook.class.getDeclaredMethod("microUsd", String.class, long.class, long.class);
            m.setAccessible(true);
            return (long) m.invoke(hook, "m", 1_000_000L, 0L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldProvideBeanThroughAssembly() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("buzhouPricingTable");
                });
    }
}
