package io.github.chyuan_cuihongyuan.buzhou.guard.config;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Fact;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 事实衰减装配测试（spec 626 / T902–T903 / impl 479）：yml 声明 half-life-turns 即
 * 包装 DecayingFactStore（经 module.factStore() 行为可见）；缺省不包装零变化。
 */
class FactDecayAssemblyTest {

    /** yml 声明：factStore 带半衰过滤（陈年低置信不注入）。 */
    @Test
    void ymlDeclarationWrapsDecayingFactStore() {
        new ApplicationContextRunner()
                .withUserConfiguration(BuzhouGuardAutoConfiguration.class)
                .withBean(BuzhouStores.class, Buzhou::inMemoryStores)
                .withPropertyValues(
                        "buzhou.guard.fact-decay.half-life-turns=4",
                        "buzhou.guard.fact-decay.floor=0.25")
                .run(context -> {
                    var module = context.getBean(io.github.chyuan_cuihongyuan.buzhou.guard.GuardModule.class);
                    var store = module.factStore();
                    store.save("s", new Fact("fact.p.fresh", "v", "p", 0, 1000, 1.0));
                    store.save("s", new Fact("fact.p.lowconf", "v", "p", 0, 1000, 0.4));
                    assertThat(store.activeFacts("s", 4)).hasSize(1); // 0.4×2^(−1)=0.2 < 0.25 滤
                    assertThat(store.activeFacts("s", 4).get(0).key()).isEqualTo("fact.p.fresh");
                });
    }

    /** 缺省：无衰减（全部注入——既有语义零变化）。 */
    @Test
    void defaultKeepsPlainFactStore() {
        new ApplicationContextRunner()
                .withUserConfiguration(BuzhouGuardAutoConfiguration.class)
                .withBean(BuzhouStores.class, Buzhou::inMemoryStores)
                .run(context -> {
                    var module = context.getBean(io.github.chyuan_cuihongyuan.buzhou.guard.GuardModule.class);
                    module.factStore().save("s", new Fact("fact.p.old", "v", "p", 0, 1000, 0.1));
                    assertThat(module.factStore().activeFacts("s", 500)).hasSize(1); // 低置信也注入
                });
    }
}
