package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 时间旅行 fork e2e（spec 311 / impl-334）：前缀复制（分支不含未来轮）、
 * 摘要不复制（未来泄漏防护）、源不动、非法 upToTurn / 空前缀拒绝。
 */
class TimeTravelForkEndToEndTest {

    /** 三轮源会话 forkFromTurn(2)：分支只带前两轮，续聊 prompt 不含第三轮。 */
    @Test
    void forkFromTurnCarriesOnlyPrefix() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("第一答：不周山");
        model.enqueueText("第二答：撑天之柱");
        model.enqueueText("第三答：未来不该出现");
        model.enqueueText("分支续答");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());

        AgentSession source = runtime.spawn("app", "agent", "sess-src");
        source.chat("第一问");
        source.chat("第二问");
        source.chat("第三问");
        source.close();

        AgentSession branch = runtime.forkFromTurn("sess-src", "app", "agent", "sess-branch", 2);
        // 复制完成即断言前缀（续聊前）：全部 ≤ 第 2 轮、无第三轮内容
        assertThat(stores.messageStore().load("sess-branch"))
                .isNotEmpty()
                .allSatisfy(m -> assertThat(m.turnSeq()).isLessThanOrEqualTo(2));
        assertThat(stores.messageStore().load("sess-branch").stream()
                .map(m -> m.content())).noneMatch(c -> c != null && c.contains("第三"));

        branch.chat("分支问");

        // 分支续聊 prompt 含前两轮、不含第三轮（未来隔离）
        String branchPrompt = model.seenPrompts.get(3).getContents().toString();
        assertThat(branchPrompt).contains("第一问").contains("不周山");
        assertThat(branchPrompt).contains("第二问").contains("撑天之柱");
        assertThat(branchPrompt).doesNotContain("第三问").doesNotContain("未来不该出现");
        // 源三轮不动
        assertThat(stores.messageStore().load("sess-src").stream()
                .mapToInt(m -> m.turnSeq()).max().orElse(0)).isEqualTo(3);
        branch.close();
    }

    /** Summary 不复制：最新摘要覆盖 upToTurn 之后轮次，复制即未来泄漏。 */
    @Test
    void summaryNotCopiedToTimeTravelBranch() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("答一");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        AgentSession source = runtime.spawn("app", "agent", "sess-src");
        source.chat("问一");
        source.close();
        stores.summaryStore().save("sess-src", new StructuredSummary("sess-src", 1,
                Map.of("future", "第三轮的结论"), 1, Instant.now()));

        AgentSession branch = runtime.forkFromTurn("sess-src", "app", "agent", "sess-tt", 1);

        assertThat(stores.summaryStore().latest("sess-tt"))
                .as("时间旅行分支不带未来摘要").isEmpty();
        assertThat(stores.summaryStore().latest("sess-src")).isPresent(); // 源不动
        branch.close();
    }

    @Test
    void rejectsInvalidUpToTurnAndEmptyPrefix() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("答");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        AgentSession source = runtime.spawn("app", "agent", "sess-src");
        source.chat("问");
        source.close();

        assertThatThrownBy(() -> runtime.forkFromTurn("sess-src", "app", "agent", "s1", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("upToTurn >= 1");
        // upToTurn 越过全部历史 = 全前缀（合法，不抛）；真正空前缀 = 源会话无消息
        assertThatThrownBy(() -> runtime.forkFromTurn("ghost", "app", "agent", "s2", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无可回放前缀");
    }

    /** 时间旅行后分支可正常续聊（管线完整可用）。 */
    @Test
    void branchUsableAfterTimeTravel() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("答");
        model.enqueueText("分支续答");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        AgentSession source = runtime.spawn("app", "agent", "sess-src");
        source.chat("问");
        source.close();

        AgentSession branch = runtime.forkFromTurn("sess-src", "app", "agent", "sess-tt", 1);
        assertThat(branch.chat("续问")).isEqualTo("分支续答");
        assertThat(model.seenPrompts).hasSize(2);
        branch.close();
    }
}
