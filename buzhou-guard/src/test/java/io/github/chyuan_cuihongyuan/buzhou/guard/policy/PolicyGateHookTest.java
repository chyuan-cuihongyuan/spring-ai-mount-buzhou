package io.github.chyuan_cuihongyuan.buzhou.guard.policy;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetrics;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.guard.taint.TaintTrackingHook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PolicyGateHook 直测（K 会话 R2 / spec 1201 / T1806）：策略门的四合同面——
 * 三态裁决映射（allow→CONTINUE / deny→Block / escalate→审批文案 Block）、
 * 结构化 Input 组装（OPA「input→decision+reason」合同：principal/arguments/taint label）、
 * policy.decided 事件字段（revision null→"" 占位）、指标 outcome 三桶。
 * 先例：TaintLifecycleStatsTest（ctx stub）、ToolDurationTimerTest（CapturingMetrics）。
 */
class PolicyGateHookTest {

    private static final String AGENT = "agent-a";
    private static final String TOOL = "deploy_prod";

    private final SessionStateStore stateStore = new InMemorySessionStateStore();

    /** 捕获 engine 收到的 Input——输入形状本身就是合同（OPA 决策入口思想）。 */
    private PolicyDecision.Input captured;

    private PolicyGateHook hookReturning(PolicyDecision decision) {
        return new PolicyGateHook(input -> {
            captured = input;
            return decision;
        });
    }

    private DefaultToolCallContext ctx() {
        HookEnvironment env = new HookEnvironment("s1", AGENT, stateStore);
        return new DefaultToolCallContext(env, "tc-1", TOOL, Map.of("target", "prod"));
    }

    @AfterEach
    void cleanup() {
        BuzhouMetricsHolder.reset();
    }

    @Test
    void allowDecisionContinuesAndForwardsStructuredInput() {
        PolicyGateHook hook = hookReturning(PolicyDecision.allow("白名单命中"));

        assertThat(hook.beforeTool(ctx())).isSameAs(HookResult.CONTINUE);

        assertThat(captured.principal()).isEqualTo(AGENT);
        assertThat(captured.toolName()).isEqualTo(TOOL);
        assertThat(captured.arguments()).containsEntry("target", "prod");
        assertThat(captured.humanApproved()).isFalse();
        assertThat(captured.labels()).containsOnlyKeys("taint").containsEntry("taint", "TRUSTED");
    }

    @Test
    void denyDecisionBlocksWithReason() {
        PolicyGateHook hook = hookReturning(PolicyDecision.deny("生产环境禁写"));

        HookResult result = hook.beforeTool(ctx());

        assertThat(result).isInstanceOf(HookResult.Block.class);
        assertThat(((HookResult.Block) result).reason()).startsWith("策略拒绝：").contains("生产环境禁写");
    }

    @Test
    void escalateDecisionBlocksWithApprovalNotice() {
        PolicyGateHook hook = hookReturning(PolicyDecision.escalate("高危操作需人工"));

        HookResult result = hook.beforeTool(ctx());

        assertThat(result).isInstanceOf(HookResult.Block.class);
        assertThat(((HookResult.Block) result).reason())
                .contains("等待人工确认（策略升级）").contains("高危操作需人工").contains("请用户审批后重试");
    }

    @Test
    void untrustedTaintStateMapsToUntrustedLabel() {
        stateStore.put("s1", new StateEntry(TaintTrackingHook.STATE_KEY,
                "UNTRUSTED:fetch_page", "test", 1, null, Instant.now()));
        PolicyGateHook hook = hookReturning(PolicyDecision.allow("放行"));

        hook.beforeTool(ctx());

        assertThat(captured.labels()).containsEntry("taint", "UNTRUSTED");
    }

    @Test
    void nonUntrustedTaintValueMapsToTrusted() {
        stateStore.put("s1", new StateEntry(TaintTrackingHook.STATE_KEY,
                "TRUSTED:read_only", "test", 1, null, Instant.now()));
        PolicyGateHook hook = hookReturning(PolicyDecision.allow("放行"));

        hook.beforeTool(ctx());

        assertThat(captured.labels()).containsEntry("taint", "TRUSTED");
    }

    @Test
    void policyDecidedEventCarriesDecisionFields() {
        List<SessionEvent> events = new ArrayList<>();
        HookEnvironment env = new HookEnvironment("s1", AGENT, stateStore);
        env.bindEventPublisher(events::add);
        DefaultToolCallContext context = new DefaultToolCallContext(env, "tc-1", TOOL, Map.of("target", "prod"));
        PolicyGateHook hook = hookReturning(new PolicyDecision(
                PolicyDecision.Action.DENY, "规则 R-1", "rev-7", Instant.now()));

        hook.beforeTool(context);

        assertThat(events).hasSize(1);
        SessionEvent event = events.get(0);
        assertThat(event.type()).isEqualTo("policy.decided");
        assertThat(event.payload())
                .containsEntry("sessionId", "s1")
                .containsEntry("toolName", TOOL)
                .containsEntry("action", "DENY")
                .containsEntry("reason", "规则 R-1")
                .containsEntry("revision", "rev-7");
    }

    @Test
    void nullRevisionEmittedAsEmptyPlaceholder() {
        List<SessionEvent> events = new ArrayList<>();
        HookEnvironment env = new HookEnvironment("s1", AGENT, stateStore);
        env.bindEventPublisher(events::add);
        DefaultToolCallContext context = new DefaultToolCallContext(env, "tc-1", TOOL, Map.of("target", "prod"));
        PolicyGateHook hook = hookReturning(new PolicyDecision(
                PolicyDecision.Action.ALLOW, "静态策略", null, null));

        hook.beforeTool(context);

        assertThat(events.get(0).payload()).containsEntry("revision", "");
    }

    @Test
    void metricsCounterTaggedByOutcome() {
        PolicyGateHook allowHook = hookReturning(PolicyDecision.allow("a"));
        PolicyGateHook denyHook = hookReturning(PolicyDecision.deny("d"));
        PolicyGateHook escalateHook = hookReturning(PolicyDecision.escalate("e"));

        ConcurrentLinkedQueue<String> counters = installCapturingMetrics();
        allowHook.beforeTool(ctx());
        denyHook.beforeTool(ctx());
        escalateHook.beforeTool(ctx());

        // 实际合同：tag 值 = Action 名小写（allow|deny|escalate）——
        // 源注释「allowed|blocked|escalated」为过期失真，T1808 单列修正
        assertThat(counters).containsExactlyInAnyOrder(
                "buzhou.guard.checks:outcome=allow",
                "buzhou.guard.checks:outcome=deny",
                "buzhou.guard.checks:outcome=escalate");
    }

    @Test
    void nameAndOrderContract() {
        PolicyGateHook hook = hookReturning(PolicyDecision.allow("a"));

        assertThat(hook.name()).isEqualTo("PolicyGateHook");
        // taint 写门(250)之后、HITL 门(300)之前——策略作为泛化授权层的链位
        assertThat(hook.order()).isEqualTo(275);
    }

    private ConcurrentLinkedQueue<String> installCapturingMetrics() {
        ConcurrentLinkedQueue<String> counters = new ConcurrentLinkedQueue<>();
        BuzhouMetricsHolder.install(new BuzhouMetrics() {
            @Override
            public void counter(String name, long delta, String... tagKeyValue) {
                counters.add(name + ":" + String.join("=", tagKeyValue));
            }

            @Override
            public void timer(String name, Duration duration, String... tagKeyValue) {
                // 本轮只断言 counter 桶
            }
        });
        return counters;
    }
}
