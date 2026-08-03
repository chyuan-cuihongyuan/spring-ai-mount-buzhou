package io.github.chyuan_cuihongyuan.buzhou.spill;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Flux;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class LongContentGuardEndToEndTest {

    @TempDir
    Path spillRoot;

    @TempDir
    Path sandboxRoot;

    static class ScriptedChatModel implements ChatModel {
        final Queue<ChatResponse> script = new ConcurrentLinkedQueue<>();
        final List<String> seenPrompts = new CopyOnWriteArrayList<>();

        void enqueue(AssistantMessage message) {
            script.add(new ChatResponse(List.of(new Generation(message))));
        }

        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            seenPrompts.add(prompt.getInstructions().toString());
            ChatResponse next = script.poll();
            return next != null ? next
                    : new ChatResponse(List.of(new Generation(new AssistantMessage("default"))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }

    static AssistantMessage toolCall(String id, String name, String argsJson) {
        return AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(id, "function", name, argsJson)))
                .build();
    }

    /** 把路径转成 JSON 安全形式：Windows 反斜杠转正斜杠，避免 Jackson \U 转义错误。 */
    static String jsonPath(java.nio.file.Path path) {
        return path.toString().replace('\\', '/');
    }

    static ToolCallback fixedTool(String name, String result) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description("d")
                        .inputSchema("{\"type\":\"object\"}").build();
            }

            @Override
            public String call(String toolInput) {
                return result;
            }
        };
    }

    static class RecordingTool implements ToolCallback {
        final List<String> receivedInputs = new CopyOnWriteArrayList<>();
        private final String name;

        RecordingTool(String name) {
            this.name = name;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder().name(name).description("d")
                    .inputSchema("{\"type\":\"object\"}").build();
        }

        @Override
        public String call(String toolInput) {
            receivedInputs.add(toolInput);
            return "written";
        }
    }

    private AgentRuntime runtime(ScriptedChatModel model, RuntimeConfig config, ToolCallback... tools) {
        BuzhouStores stores = Buzhou.inMemoryStores();
        return Buzhou.runtime(model, stores, config, tools);
    }

    @Test
    void longResultReplacedByHandleModelNeverSeesOriginal() {
        String big = "机密长文".repeat(2000);
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(toolCall("tc-1", "big_tool", "{}"));
        model.enqueue(new AssistantMessage("已读完"));
        SpillModule spill = SpillModule.withDefaults(spillRoot);
        SpillGuardModule guard = SpillGuardModule.fromModule(spill, sandboxRoot)
                .thresholdChars(1000).build();
        AgentRuntime runtime = runtime(model,
                RuntimeConfig.merge(spill.configure(), guard.configure()), fixedTool("big_tool", big));
        AgentSession session = runtime.spawn("app", "agent", "sess-offload");

        String reply = session.chat("读取大数据");

        assertThat(reply).isEqualTo("已读完");
        String secondPrompt = model.seenPrompts.get(1);
        assertThat(secondPrompt).contains("spill://agent/sess-offload/").contains("read_range");
        assertThat(secondPrompt).doesNotContain(big);
        session.close();
    }

    @Test
    void toolReceivesFullContentWhenModelPassesOnlyPath() throws Exception {
        Path fullFile = sandboxRoot.resolve("full.etl");
        String fullContent = "全文内容".repeat(500);
        Files.writeString(fullFile, fullContent);
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(toolCall("tc-1", "write_file",
                "{\"path\":\"out.etl\",\"contentPath\":\"" + jsonPath(fullFile) + "\"}"));
        model.enqueue(new AssistantMessage("已写入"));
        RecordingTool writeFile = new RecordingTool("write_file");
        SpillModule spill = SpillModule.withDefaults(spillRoot);
        SpillGuardModule guard = SpillGuardModule.fromModule(spill, sandboxRoot)
                .longContentParam("write_file", "content", "contentPath").build();
        AgentRuntime runtime = runtime(model,
                RuntimeConfig.merge(spill.configure(), guard.configure()), writeFile);
        AgentSession session = runtime.spawn("app", "agent", "sess-onload");

        session.chat("把 full.etl 的内容写到 out.etl");

        assertThat(writeFile.receivedInputs).hasSize(1);
        String received = writeFile.receivedInputs.get(0);
        assertThat(received).contains(fullContent.substring(0, 100));
        assertThat(received).doesNotContain("contentPath");
        session.close();
    }

    @Test
    void onloadFailureBlocksToolCallAndEmitsEvent() throws Exception {
        Path outsideFile = Files.writeString(spillRoot.resolve("secret.txt"), "secret");
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(toolCall("tc-1", "write_file",
                "{\"path\":\"out.txt\",\"contentPath\":\"" + jsonPath(outsideFile) + "\"}"));
        model.enqueue(new AssistantMessage("路径被拒，我改用内联内容"));
        RecordingTool writeFile = new RecordingTool("write_file");
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        SpillModule spill = SpillModule.withDefaults(spillRoot);
        SpillGuardModule guard = SpillGuardModule.fromModule(spill, sandboxRoot)
                .longContentParam("write_file", "content", "contentPath").build();
        AgentRuntime runtime = runtime(model,
                RuntimeConfig.merge(spill.configure(), guard.configure()), writeFile);
        AgentSession session = runtime.spawn("app", "agent", "sess-onload-fail");
        session.addEventListener(events::add);

        String reply = session.chat("写文件");

        assertThat(reply).isEqualTo("路径被拒，我改用内联内容");
        assertThat(writeFile.receivedInputs).isEmpty();
        assertThat(model.seenPrompts.get(1)).contains("写侧加载失败");
        assertThat(events).anyMatch(e -> e.type().equals("onload.failed"));
        session.close();
    }

    @Test
    void offloadFailurePassesOriginalThroughWithoutBlocking() {
        String big = "透传原文".repeat(2000);
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(toolCall("tc-1", "big_tool", "{}"));
        model.enqueue(new AssistantMessage("拿到原文"));
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        SpillService failingService = new SpillService(new SpillOffloadHookTest.FailingSpillStore(), 64, 3);
        SpillGuardModule guard = SpillGuardModule.builder(failingService, uri -> null, sandboxRoot)
                .thresholdChars(1000).build();
        AgentRuntime runtime = runtime(model, guard.configure(), fixedTool("big_tool", big));
        AgentSession session = runtime.spawn("app", "agent", "sess-degraded");
        session.addEventListener(events::add);

        String reply = session.chat("读取大数据");

        assertThat(reply).isEqualTo("拿到原文");
        assertThat(model.seenPrompts.get(1)).contains("透传原文".repeat(10));
        assertThat(events).anyMatch(e -> e.type().equals("offload.degraded"));
        session.close();
    }

    @Test
    void directEditOfSnapshotBlockedThenCopyAndEditSucceeds() throws Exception {
        Path snapshot = sandboxRoot.resolve("snapshot.txt");
        Files.writeString(snapshot, "原始内容待修改");
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(toolCall("tc-1", "str_replace",
                "{\"path\":\"" + jsonPath(snapshot) + "\",\"oldStr\":\"原始\",\"newStr\":\"篡改\"}"));
        Path workCopy = sandboxRoot.resolve("work.txt");
        model.enqueue(toolCall("tc-2", "copy_file",
                "{\"srcPath\":\"" + jsonPath(snapshot) + "\",\"destPath\":\"" + jsonPath(workCopy) + "\"}"));
        model.enqueue(toolCall("tc-3", "str_replace",
                "{\"path\":\"" + jsonPath(workCopy) + "\",\"oldStr\":\"原始\",\"newStr\":\"已改\"}"));
        model.enqueue(new AssistantMessage("编辑完成"));
        SpillModule spill = SpillModule.withDefaults(spillRoot);
        SpillGuardModule guard = SpillGuardModule.fromModule(spill, sandboxRoot).build();
        AgentRuntime runtime = runtime(model,
                RuntimeConfig.merge(spill.configure(), guard.configure()));
        AgentSession session = runtime.spawn("app", "agent", "sess-cow");
        guard.readOnlyRegistry().register("sess-cow", snapshot);

        String reply = session.chat("修改快照内容");

        assertThat(reply).isEqualTo("编辑完成");
        assertThat(Files.readString(snapshot)).isEqualTo("原始内容待修改");
        assertThat(Files.readString(workCopy)).isEqualTo("已改内容待修改");
        assertThat(model.seenPrompts.get(1)).contains("只读");
        session.close();
    }
}
