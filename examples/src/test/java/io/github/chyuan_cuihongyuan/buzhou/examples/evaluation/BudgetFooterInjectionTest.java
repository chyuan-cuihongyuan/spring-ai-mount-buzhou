package io.github.chyuan_cuihongyuan.buzhou.examples.evaluation;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.examples.support.TroubleshootingFixture;
import io.github.chyuan_cuihongyuan.buzhou.memory.MemoryModule;
import io.github.chyuan_cuihongyuan.buzhou.memory.budget.SegmentBudgetPlanner;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T23 端到端（docs/spec/11 memory，主接缝）：压缩发生后，注入视图的摘要块携带
 * 每段 chars_current/chars_limit 预算页脚 + 头部预算提示——模型可感知预算压力、自削 P3。
 */
class BudgetFooterInjectionTest {

    @Test
    void injectedSummaryCarriesPerSectionBudgetFooters() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = "budget-footer-e2e";
        stores.messageStore().append(sid, TroubleshootingFixture.troubleshootingHistory(sid, 20));

        var summaryModel = new scriptingModel(TroubleshootingFixture.NINE_SECTIONS);
        var mainModel = new scriptingModel("继续排查中");
        var config = MemoryModule.configure(TroubleshootingFixture.smallWindowYml(),
                stores, mainModel, summaryModel);
        AgentRuntime runtime = Buzhou.runtime(mainModel, stores, config);

        AgentSession session = runtime.spawn("budget-app", "support-agent", sid);
        mainModel.enqueueText("继续排查");
        session.chat("继续排查");
        mainModel.enqueueText("继续排查");
        session.chat("再看看");
        session.close();

        assertThat(stores.summaryStore().latest(sid)).isPresent();
        String view = mainModel.seenPrompts.get(0).getInstructions().toString();
        assertThat(view).contains("<system-reminder>");
        // 头部预算提示（引导模型优先削 P3）
        assertThat(view).contains(SegmentBudgetPlanner.BUDGET_HEADER);
        // 每段页脚：当前/上限 字符
        assertThat(view).contains("（本段 ");
        assertThat(view).contains(" 字符）");
        // P0 段正文仍保真（页脚渲染不破坏内容）
        assertThat(view).contains(TroubleshootingFixture.ORDER_ID);
    }

    /** 极简脚本模型：固定回复 + 记录 prompt。 */
    static final class scriptingModel implements org.springframework.ai.chat.model.ChatModel {
        final List<Prompt> seenPrompts = new java.util.ArrayList<>();
        private final String reply;

        scriptingModel(String reply) {
            this.reply = reply;
        }

        void enqueueText(String text) {
            // 脚本模型固定回复，无需队列
        }

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder().build();
        }

        @Override
        public org.springframework.ai.chat.model.ChatResponse call(Prompt prompt) {
            seenPrompts.add(prompt);
            return new org.springframework.ai.chat.model.ChatResponse(
                    List.of(new org.springframework.ai.chat.model.Generation(new AssistantMessage(reply))));
        }

        @Override
        public Flux<org.springframework.ai.chat.model.ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }
}
