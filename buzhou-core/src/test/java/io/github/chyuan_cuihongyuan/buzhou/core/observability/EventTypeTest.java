package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * EventType 直测（spec 1200 / T1801 / K 会话 R1 补测——此前零覆盖：既有测试直写字符串
 * 字面量，未断言常量与注册表逻辑）。
 *
 * <p>断言内置常量经 of 反查恒等（注册表初始化完整性）、自定义值 lookup-or-create 幂等
 * 与非法入参拒绝。
 */
class EventTypeTest {

    private static final String[] BUILT_INS = {
            EventType.THINKING, EventType.FINAL_REPLY, EventType.TOOL_INPUT,
            EventType.TOOL_OUTPUT, EventType.ERROR, EventType.DANGLING_REPAIR,
            EventType.HITL_REQUEST, EventType.HITL_DECISION, EventType.GUARD_ACTION,
            EventType.STREAM_FIRST_TOKEN };

    private static final String CUSTOM_TYPE = "K_TEST_CUSTOM_EVENT";

    @Test
    void allBuiltInConstantsResolveToSameInstance() {
        for (String builtIn : BUILT_INS) {
            assertThat(EventType.of(builtIn)).isSameAs(builtIn);
        }
        assertThat(BUILT_INS).hasSize(10);
    }

    @Test
    void customValueIsRegisteredAndIdempotent() {
        String first = EventType.of(CUSTOM_TYPE);
        String second = EventType.of(CUSTOM_TYPE);
        assertThat(first).isEqualTo(CUSTOM_TYPE);
        assertThat(second).isSameAs(first);
    }

    @Test
    void blankOrNullValueIsRejected() {
        assertThatThrownBy(() -> EventType.of(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EventType.of("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void builtInValuesHaveExactStringForms() {
        assertThat(EventType.THINKING).isEqualTo("THINKING");
        assertThat(EventType.FINAL_REPLY).isEqualTo("FINAL_REPLY");
        assertThat(EventType.TOOL_INPUT).isEqualTo("TOOL_INPUT");
        assertThat(EventType.TOOL_OUTPUT).isEqualTo("TOOL_OUTPUT");
        assertThat(EventType.ERROR).isEqualTo("ERROR");
        assertThat(EventType.STREAM_FIRST_TOKEN).isEqualTo("STREAM_FIRST_TOKEN");
    }
}
