package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OnFail 值序稳定护栏直测（spec 1200 / T1801 / K 会话 R1 补测——此前零覆盖）。
 *
 * <p>枚举值序与 name 是消费方 switch/配置解析的隐式契约——防误删/误改名/误排序。
 */
class OnFailTest {

    @Test
    void valueSetAndOrderAreStable() {
        assertThat(OnFail.values()).extracting(Enum::name)
                .containsExactly("FILTER", "REFRAIN", "EXCEPTION", "REASK");
    }

    @Test
    void valueOfRoundTripForEachConstant() {
        for (OnFail onFail : OnFail.values()) {
            assertThat(OnFail.valueOf(onFail.name())).isSameAs(onFail);
        }
    }
}
