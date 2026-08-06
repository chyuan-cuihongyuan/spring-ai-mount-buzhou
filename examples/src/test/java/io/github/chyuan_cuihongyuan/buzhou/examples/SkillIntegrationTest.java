package io.github.chyuan_cuihongyuan.buzhou.examples;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.memory.MemoryModule;
import io.github.chyuan_cuihongyuan.buzhou.skill.SkillModule;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Skill 体系跨机制集成测试（ticket 14 checklist + spec 04 入参校验/资源读取）。
 *
 * <p>贯穿 skills（扫描/清单/工具/资源解析）→ core（装配、ToolContext sessionId 传递）→
 * memory（注入视图）→ spill（read_range 接管 skill://）四模块。feature 模块间禁止直接依赖，
 * 故跨机制端到端落 examples 聚合侧（09 模块工程档依赖白名单）。
 */
class SkillIntegrationTest {

    @TempDir
    Path spillDir;

    @Test
    void classpathCatalogInjectedAndLoadSkillReturnsBody() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();

        SkillModule skills = SkillModule.builder().build();
        RuntimeConfig config = RuntimeConfig.merge(
                skills.configure(),
                MemoryModule.configure(Map.of(), stores, model, model, null, skills.catalogRenderer()));
        AgentRuntime runtime = Buzhou.runtime(model, stores, config);

        // 轮 1：模型先调 load_skill("code-review")，工具返回后再给最终回复
        model.enqueue(AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "tc-1", "function", "load_skill", "{\"name\":\"code-review\"}")))
                .build());
        model.enqueue(new AssistantMessage("已加载评审技能，开始评审"));

        AgentSession session = runtime.spawn("app", "agent", "sess-skill");
        session.chat("帮我评审代码");
        session.close();

        // checkbox 1：模型首次调用收到的 prompt 含 Skill Catalog system-reminder 块
        Prompt firstCall = model.seenPrompts.get(0);
        assertThat(firstCall.getInstructions())
                .anyMatch(m -> contains(m, "可用技能") && contains(m, "code-review")
                        && contains(m, "load_skill(name)"));

        // checkbox 2：load_skill 工具结果（第二次调用 prompt 中的 ToolResponseMessage）含正文 + 资源清单
        Prompt secondCall = model.seenPrompts.get(1);
        assertThat(secondCall.getInstructions())
                .anyMatch(m -> contains(m, "# Code Review Skill") && contains(m, "checklists/security.md"));
    }

    @Test
    void bindingChangeCropsCatalogNextTurn() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        io.github.chyuan_cuihongyuan.buzhou.core.internal.policy.InMemoryBindingPolicyStore bindingStore =
                new io.github.chyuan_cuihongyuan.buzhou.core.internal.policy.InMemoryBindingPolicyStore();

        SkillModule skills = SkillModule.builder().bindingStore(bindingStore).build();
        RuntimeConfig config = RuntimeConfig.merge(
                skills.configure(),
                MemoryModule.configure(Map.of(), stores, model, model, null, skills.catalogRenderer()));
        AgentRuntime runtime = Buzhou.runtime(model, stores, config);

        // 改绑定：仅 sql-tuning 可见（裁剪掉 code-review）
        skills.skillAdminApi().setBinding("app", "agent", List.of("sql-tuning"));

        model.enqueue(new AssistantMessage("ok"));
        AgentSession session = runtime.spawn("app", "agent", "sess-bind");
        session.chat("看下清单");
        session.close();

        Prompt firstCall = model.seenPrompts.get(0);
        assertThat(firstCall.getInstructions())
                .anyMatch(m -> contains(m, "sql-tuning") && contains(m, "可用技能"));
        // code-review 被裁剪
        assertThat(firstCall.getInstructions())
                .noneMatch(m -> contains(m, "code-review"));
    }

    @Test
    void loadSkillRejectsSkillCroppedByBinding() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        io.github.chyuan_cuihongyuan.buzhou.core.internal.policy.InMemoryBindingPolicyStore bindingStore =
                new io.github.chyuan_cuihongyuan.buzhou.core.internal.policy.InMemoryBindingPolicyStore();

        SkillModule skills = SkillModule.builder().bindingStore(bindingStore).build();
        RuntimeConfig config = RuntimeConfig.merge(
                skills.configure(),
                MemoryModule.configure(Map.of(), stores, model, model, null, skills.catalogRenderer()));
        AgentRuntime runtime = Buzhou.runtime(model, stores, config);

        // 绑定仅 sql-tuning；模型执意按名加载被裁剪的 code-review
        skills.skillAdminApi().setBinding("app", "agent", List.of("sql-tuning"));
        model.enqueue(AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "tc-1", "function", "load_skill", "{\"name\":\"code-review\"}")))
                .build());
        model.enqueue(new AssistantMessage("技能不可用"));

        AgentSession session = runtime.spawn("app", "agent", "sess-guarded");
        session.chat("加载 code-review");
        session.close();

        // spec 04 入参校验：name 必须在当前绑定清单内 → 工具返回「技能不存在或未绑定」文本
        Prompt secondCall = model.seenPrompts.get(1);
        assertThat(secondCall.getInstructions())
                .anyMatch(m -> m instanceof ToolResponseMessage
                        && contains(m, "技能不存在或未绑定") && contains(m, "code-review"));
        // 且正文未被带出
        assertThat(secondCall.getInstructions())
                .noneMatch(m -> contains(m, "# Code Review Skill"));
    }

    @Test
    void skillResourceReadViaReadRangeSkillUri() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();

        SkillModule skills = SkillModule.builder().build();
        SpillModule spill = SpillModule.withDefaults(spillDir)
                .skillResourceResolver(skills.skillResourceResolver());
        RuntimeConfig config = RuntimeConfig.merge(
                skills.configure(),
                spill.configure(),
                MemoryModule.configure(Map.of(), stores, model, model, null, skills.catalogRenderer()));
        AgentRuntime runtime = Buzhou.runtime(model, stores, config);

        // 模型按 load_skill 指引的 skill:// 路径调 read_range 取资源内容
        model.enqueue(AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "tc-1", "function", "read_range",
                        "{\"path\":\"skill://code-review/checklists/security.md\",\"mode\":\"bytes\"}")))
                .build());
        model.enqueue(new AssistantMessage("已取到核查清单"));

        AgentSession session = runtime.spawn("app", "agent", "sess-res");
        session.chat("看下安全核查清单");
        session.close();

        Prompt secondCall = model.seenPrompts.get(1);
        assertThat(secondCall.getInstructions())
                .anyMatch(m -> m instanceof ToolResponseMessage
                        && contains(m, "输入是否经校验/转义"));
    }

    @Test
    void skillResourceReadRejectedWhenCropped() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        io.github.chyuan_cuihongyuan.buzhou.core.internal.policy.InMemoryBindingPolicyStore bindingStore =
                new io.github.chyuan_cuihongyuan.buzhou.core.internal.policy.InMemoryBindingPolicyStore();

        SkillModule skills = SkillModule.builder().bindingStore(bindingStore).build();
        SpillModule spill = SpillModule.withDefaults(spillDir)
                .skillResourceResolver(skills.skillResourceResolver());
        RuntimeConfig config = RuntimeConfig.merge(
                skills.configure(),
                spill.configure(),
                MemoryModule.configure(Map.of(), stores, model, model, null, skills.catalogRenderer()));
        AgentRuntime runtime = Buzhou.runtime(model, stores, config);

        // 绑定仅 sql-tuning；模型执意读被裁剪技能的资源
        skills.skillAdminApi().setBinding("app", "agent", List.of("sql-tuning"));
        model.enqueue(AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "tc-1", "function", "read_range",
                        "{\"path\":\"skill://code-review/checklists/security.md\",\"mode\":\"bytes\"}")))
                .build());
        model.enqueue(new AssistantMessage("资源不可用"));

        AgentSession session = runtime.spawn("app", "agent", "sess-res-guarded");
        session.chat("读安全核查清单");
        session.close();

        // 与 load_skill 同源的绑定校验：拒绝且内容不带出
        Prompt secondCall = model.seenPrompts.get(1);
        assertThat(secondCall.getInstructions())
                .anyMatch(m -> m instanceof ToolResponseMessage
                        && contains(m, "技能资源不存在或未绑定"));
        assertThat(secondCall.getInstructions())
                .noneMatch(m -> contains(m, "输入是否经校验"));
    }

    /** 工具结果在 ToolResponseMessage.getResponses() 里（getText() 恒为空串），文本消息直接取 getText()。 */
    private static boolean contains(Message m, String text) {
        if (m instanceof ToolResponseMessage trm) {
            return trm.getResponses().stream()
                    .anyMatch(r -> r.responseData() != null && r.responseData().contains(text));
        }
        return m.getText() != null && m.getText().contains(text);
    }

    static class ScriptedChatModel implements ChatModel {
        final Queue<ChatResponse> script = new ConcurrentLinkedQueue<>();
        final List<Prompt> seenPrompts = new CopyOnWriteArrayList<>();

        void enqueue(AssistantMessage message) {
            script.add(new ChatResponse(List.of(new Generation(message))));
        }

        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            seenPrompts.add(prompt);
            ChatResponse next = script.poll();
            if (next == null) {
                next = new ChatResponse(List.of(new Generation(new AssistantMessage("default"))));
            }
            return next;
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }
}
