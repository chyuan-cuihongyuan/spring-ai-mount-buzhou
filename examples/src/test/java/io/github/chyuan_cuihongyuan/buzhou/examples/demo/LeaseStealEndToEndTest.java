package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.LeaseLostException;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.FakeChatModel;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptStep;
import io.github.chyuan_cuihongyuan.buzhou.examples.support.FakeModelGuard;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * impl-33 / spec 13 §core-3 端到端：多实例双主防护——外部进程（另一实例 spawn with steal /
 * 运维接管）把本会话租约直接抢走后：
 *
 * <ul>
 *   <li>本地在途 Turn 立即以 {@link LeaseLostException} 中止（fence 校验发现 fencingToken
 *       已易主）；</li>
 *   <li>消息台账零新增写入（在飞工具结果与终局回复都不落库——双主窗口内本地不写脏数据）；</li>
 *   <li>后续 chat() 得到明确错误（会话已失去租约），不静默复活；</li>
 *   <li>租约正常持有时既有行为不回归（正常工具轮照旧落库）。</li>
 * </ul>
 */
class LeaseStealEndToEndTest {

    private static final String SESSION_ID = "steal-e2e";

    /** 执行中从「外部进程」身份直接抢走本会话租约（store 层 steal，绕过本运行时）。 */
    private static ToolCallback externallyStealingTool(BuzhouStores stores) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("external_takeover").description("外部接管")
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
            }

            @Override
            public String call(String toolInput) {
                stores.sessionLeaseStore().steal(SESSION_ID, "another-process", Duration.ofSeconds(90));
                return "takeover-happened";
            }
        };
    }

    private static ToolCallback fixedTool(String name) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description(name)
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
            }

            @Override
            public String call(String toolInput) {
                return name + "-result";
            }
        };
    }

    @Test
    void shouldAbortLocalTurnWithoutLedgerWrites_whenExternalProcessStealsLeaseMidTurn() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        FakeChatModel model = FakeChatModel.script(
                ScriptStep.toolCall("external_takeover", "{}"),
                ScriptStep.text("不应出现的终局"));
        FakeModelGuard.requireTestDouble(model);
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults(),
                externallyStealingTool(stores));

        AgentSession session = runtime.spawn("lease-app", "support-agent", SESSION_ID);
        Throwable thrown = catchThrowable(() -> session.chat("开始处理"));

        // 本地 Turn 立即中止：结构化 LeaseLost（NON_RETRYABLE——双主窗口零写入）
        assertThat(thrown).isInstanceOf(LeaseLostException.class);
        assertThat(((LeaseLostException) thrown).errorCode()).isEqualTo(ErrorCode.LEASE_LOST);
        // 第 2 轮模型调用从未发生（工具批完成后 fence 即拦截）
        assertThat(model.callCount()).isEqualTo(1);
        // 消息台账零新增：在飞工具结果不落库、终局回复不落库
        List<BuzhouMessage> ledger = stores.messageStore().load(SESSION_ID);
        assertThat(ledger.stream().filter(m -> m.role() == Role.TOOL))
                .as("被抢走后在飞工具结果不得入台账")
                .isEmpty();
        assertThat(ledger).extracting(BuzhouMessage::content)
                .noneMatch(c -> c != null && c.contains("不应出现的终局"));
        // 后续调用得到明确错误（会话已失去租约），不静默复活
        assertThat(catchThrowable(() -> session.chat("继续")))
                .isInstanceOf(LeaseLostException.class)
                .hasMessageContaining(SESSION_ID);
        session.close();
    }

    @Test
    void shouldCompleteTurnAndPersistLedger_whenLeaseHeld() {
        // 回归：租约正常持有时，工具轮与台账写入照旧（fence/续租不干扰健康路径）
        BuzhouStores stores = Buzhou.inMemoryStores();
        FakeChatModel model = FakeChatModel.script(
                ScriptStep.toolCall("healthy_tool", "{}"),
                ScriptStep.text("正常终局"));
        FakeModelGuard.requireTestDouble(model);
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults(),
                fixedTool("healthy_tool"));

        AgentSession session = runtime.spawn("lease-app", "support-agent", "steal-e2e-healthy");
        String reply = session.chat("正常调用");
        session.close();

        assertThat(reply).isEqualTo("正常终局");
        List<BuzhouMessage> ledger = stores.messageStore().load("steal-e2e-healthy");
        assertThat(ledger.stream().filter(m -> m.role() == Role.TOOL)).hasSize(1);
        assertThat(ledger).extracting(BuzhouMessage::content)
                .anySatisfy(c -> assertThat(c).contains("healthy_tool-result"));
    }
}
