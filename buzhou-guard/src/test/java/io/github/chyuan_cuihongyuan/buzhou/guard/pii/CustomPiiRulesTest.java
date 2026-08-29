package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 118 §B / T418：自定义 PII 规则红队——命名正则命中 [PII:NAME] 占位符；与
 * 内置五型叠加；幂等；规则名校验 fail-fast；无规则构造零变化。
 * Presidio PatternRecognizer 对应物（spec 86 fog 收口）。
 */
class CustomPiiRulesTest {

    @Test
    void customRuleRedactsWithStablePlaceholder() {
        CustomPiiRules rules = new CustomPiiRules(List.of(
                CustomPiiRules.Rule.of("ORDER_ID", "ORD-\\d{6,10}"),
                CustomPiiRules.Rule.of("STAFF_NO", "EMP\\.[A-Z]\\d{4}")));

        assertThat(rules.redact("订单 ORD-2026010233 已发货，经办 EMP.Z0421"))
                .isEqualTo("订单 [PII:ORDER_ID] 已发货，经办 [PII:STAFF_NO]");
        // 幂等：占位符不再处理；无命中原文返回
        assertThat(rules.redact("[PII:ORDER_ID] 已发货，另有 EMP.Q0001")).isEqualTo("[PII:ORDER_ID] 已发货，另有 [PII:STAFF_NO]"); // 叠加继续脱
        assertThat(rules.redact("普通内容")).isEqualTo("普通内容");
    }

    @Test
    void hookStacksCustomOnBuiltinAndValidatesNames() {
        PiiRedactionHook hook = new PiiRedactionHook(java.util.EnumSet.allOf(PiiType.class),
                new CustomPiiRules(List.of(CustomPiiRules.Rule.of("ORDER_ID", "ORD-\\d{6,}"))));
        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
        DefaultToolCallContext ctx = new DefaultToolCallContext(env, "tc1", "erp", Map.of());
        ctx.markExecuted("联系 a@b.co，订单 ORD-998877665", null);

        hook.afterTool(ctx);

        assertThat(String.valueOf(ctx.result()))
                .isEqualTo("联系 [PII:EMAIL]，订单 [PII:ORDER_ID]"); // 内置 + 自定义叠加

        assertThatThrownBy(() -> CustomPiiRules.Rule.of("bad name", "x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CustomPiiRules.Rule.of(null, "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noRulesConstructorKeepsBehaviorUnchanged() {
        PiiRedactionHook plain = new PiiRedactionHook();
        HookEnvironment env = new HookEnvironment("s2", "agent", new InMemorySessionStateStore());
        DefaultToolCallContext ctx = new DefaultToolCallContext(env, "tc2", "erp", Map.of());
        ctx.markExecuted("邮箱 a@b.co", null);

        plain.afterTool(ctx);

        assertThat(String.valueOf(ctx.result())).isEqualTo("邮箱 [PII:EMAIL]"); // 既有行为
    }
}
