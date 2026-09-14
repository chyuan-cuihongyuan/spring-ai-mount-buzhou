package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.guard.GuardExemptionRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PII 脱敏豁免征询测试（spec 1627 / T2405–T2406 / impl 1180）：双粒度——
 * 工具级（该工具输出整体豁免）与类型级（type:TYPE 该类型不脱敏，其余照脱）；
 * 无豁免行为零变化。
 */
class PiiExemptionTest {

    private static ToolCallContext toolCall(String tool, Object result) {
        return new ToolCallContext() {
            @Override public String sessionId() { return "s1"; }
            @Override public String agentName() { return "agent"; }
            @Override public int turn() { return 1; }
            @Override public io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle state() { return null; }
            @Override public void emitEvent(SessionEvent event) { }
            @Override public String toolName() { return tool; }
            @Override public String toolCallId() { return "tc1"; }
            @Override public Map<String, Object> arguments() { return Map.of(); }
            private Object resultValue = result;
            @Override public Object result() { return resultValue; }
            @Override public Throwable error() { return null; }
            @Override public void replaceArguments(Map<String, Object> newArguments) { }
            @Override public void replaceResult(Object newResult) { resultValue = newResult; }
        };
    }

    private static final String EMAIL_AND_PHONE =
            "联系 test@example.com 或 13800138000";

    @Test
    void toolLevelExemptionSkipsRedactionEntirely() {
        GuardExemptionRegistry exemptions = new GuardExemptionRegistry();
        exemptions.grant("pii-redaction", "trusted_scraper",
                System.currentTimeMillis() + 60_000, "输出已人工核验");
        PiiRedactionHook hook = new PiiRedactionHook(null, null, false, exemptions);
        PiiHitStats.global().reset();
        ToolCallContext ctx = toolCall("trusted_scraper", EMAIL_AND_PHONE);
        hook.afterTool(ctx);
        assertThat(ctx.result()).isEqualTo(EMAIL_AND_PHONE); // 原样透传
        assertThat(PiiHitStats.global().exemptionsApplied()).isEqualTo(1); // spec 1640：豁免计数
    }

    @Test
    void typeLevelExemptionRedactsOnlyNonExemptTypes() {
        GuardExemptionRegistry exemptions = new GuardExemptionRegistry();
        exemptions.grant("pii-redaction", "type:CN_PHONE",
                System.currentTimeMillis() + 60_000, "电话误报率高");
        PiiRedactionHook hook = new PiiRedactionHook(null, null, false, exemptions);
        ToolCallContext ctx = toolCall("any_tool", EMAIL_AND_PHONE);
        hook.afterTool(ctx);
        String out = String.valueOf(ctx.result());
        assertThat(out).contains("[PII:");           // EMAIL 仍脱敏
        assertThat(out).contains("13800138000");     // PHONE 豁免——原文保留
    }

    @Test
    void noExemptionKeepsBaselineRedaction() {
        PiiRedactionHook hook = new PiiRedactionHook(null, null, false,
                new GuardExemptionRegistry());
        ToolCallContext ctx = toolCall("any_tool", EMAIL_AND_PHONE);
        hook.afterTool(ctx);
        String out = String.valueOf(ctx.result());
        assertThat(out).contains("[PII:EMAIL");
        assertThat(out).contains("[PII:CN_PHONE");
        assertThat(out).doesNotContain("13800138000");
    }
}
