package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultTurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.guard.GuardModule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 129 / T476：PII yml 声明式规则——custom-rules（List<{name,pattern}> / map
 * 形态）经 GuardModule.fromYml 进输出与输入两侧 hook；无键零变化；非法名/坏正则
 * 装配期 fail-fast。
 */
class PiiYmlCustomRulesTest {

    private HookEnvironment env() {
        return new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
    }

    private PiiRedactionHook outputHookOf(GuardModule module) {
        return module.configure().hooks().stream()
                .filter(h -> h instanceof PiiRedactionHook)
                .map(h -> (PiiRedactionHook) h)
                .findFirst().orElseThrow();
    }

    private PiiInputRedactionHook inputHookOf(GuardModule module) {
        return module.configure().hooks().stream()
                .filter(h -> h instanceof PiiInputRedactionHook)
                .map(h -> (PiiInputRedactionHook) h)
                .findFirst().orElseThrow();
    }

    @Test
    void ymlListFormFeedsBothSides() {
        GuardModule module = GuardModule.fromYml(Buzhou.inMemoryStores(),
                Map.of("pii", Map.of(
                        "enabled", true,
                        "input-redaction", true,
                        "custom-rules", List.of(
                                Map.of("name", "ORDER_ID", "pattern", "ORD-\\d{6,}")))));

        // 输出侧：内置 EMAIL + 自定义 ORDER_ID 叠加
        DefaultToolCallContext out = new DefaultToolCallContext(env(), "tc1", "fetch", Map.of());
        out.markExecuted("订单 ORD-20260001 联系 a@b.co", null);
        outputHookOf(module).afterTool(out);
        assertThat(String.valueOf(out.result()))
                .isEqualTo("订单 [PII:ORDER_ID] 联系 [PII:EMAIL]");

        // 输入侧：同份声明
        DefaultTurnContext in = new DefaultTurnContext(env(), "查 ORD-20260002 尾号");
        inputHookOf(module).beforeTurn(in);
        assertThat(in.input()).isEqualTo("查 [PII:ORDER_ID] 尾号");
    }

    @Test
    void ymlMapFormIsEquivalent() {
        GuardModule module = GuardModule.fromYml(Buzhou.inMemoryStores(),
                Map.of("pii", Map.of(
                        "enabled", true,
                        "custom-rules", Map.of("STAFF_NO", "EMP-\\d{4}"))));

        DefaultToolCallContext out = new DefaultToolCallContext(env(), "tc2", "fetch", Map.of());
        out.markExecuted("经办 EMP-0042", null);
        outputHookOf(module).afterTool(out);
        assertThat(String.valueOf(out.result())).isEqualTo("经办 [PII:STAFF_NO]");
    }

    @Test
    void noCustomRulesKeyKeepsLegacyBehavior() {
        GuardModule module = GuardModule.fromYml(Buzhou.inMemoryStores(),
                Map.of("pii", Map.of("enabled", true)));

        DefaultToolCallContext out = new DefaultToolCallContext(env(), "tc3", "fetch", Map.of());
        out.markExecuted("订单 ORD-20260001 邮箱 a@b.co", null);
        outputHookOf(module).afterTool(out);
        // 无自定义规则：订单号不脱（内置五型无此格式）、邮箱照脱
        assertThat(String.valueOf(out.result()))
                .isEqualTo("订单 ORD-20260001 邮箱 [PII:EMAIL]");
    }

    @Test
    void invalidRuleNameFailsFastAtAssembly() {
        Map<String, Object> yml = Map.of("pii", Map.of(
                "enabled", true,
                "custom-rules", List.of(Map.of("name", "小写名", "pattern", "x+"))));
        assertThatThrownBy(() -> GuardModule.fromYml(Buzhou.inMemoryStores(), yml))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("小写名");
    }

    @Test
    void malformedRegexFailsFastAtAssembly() {
        Map<String, Object> yml = Map.of("pii", Map.of(
                "enabled", true,
                "custom-rules", List.of(Map.of("name", "BAD_RE", "pattern", "[unclosed"))));
        assertThatThrownBy(() -> GuardModule.fromYml(Buzhou.inMemoryStores(), yml))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void programmaticBuilderPathAlsoFeedsBothSides() {
        CustomPiiRules rules = new CustomPiiRules(List.of(
                CustomPiiRules.Rule.of("TOKEN", "sk-[a-z0-9]{8,}")));
        GuardModule module = GuardModule.builder(Buzhou.inMemoryStores())
                .piiRedaction()
                .piiInputRedaction()
                .customPiiRules(rules)
                .build();

        DefaultToolCallContext out = new DefaultToolCallContext(env(), "tc4", "fetch", Map.of());
        out.markExecuted("密钥 sk-abcdef123456 泄漏", null);
        outputHookOf(module).afterTool(out);
        assertThat(String.valueOf(out.result())).isEqualTo("密钥 [PII:TOKEN] 泄漏");

        DefaultTurnContext in = new DefaultTurnContext(env(), "用 sk-abcdef123456 试下");
        inputHookOf(module).beforeTurn(in);
        assertThat(in.input()).isEqualTo("用 [PII:TOKEN] 试下");

        // hook 面干净：两类 hook 各一
        List<BuzhouHook> hooks = module.configure().hooks();
        assertThat(hooks.stream().filter(h -> h instanceof PiiRedactionHook).count()).isEqualTo(1);
        assertThat(hooks.stream().filter(h -> h instanceof PiiInputRedactionHook).count()).isEqualTo(1);
    }
}
