package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Transport;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.examples.support.TroubleshootingFixture;
import io.github.chyuan_cuihongyuan.buzhou.mcp.InMemoryToolSetSpecStore;
import io.github.chyuan_cuihongyuan.buzhou.mcp.McpClientRegistry;
import io.github.chyuan_cuihongyuan.buzhou.mcp.McpConnection;
import io.github.chyuan_cuihongyuan.buzhou.mcp.McpConnectionFactory;
import io.github.chyuan_cuihongyuan.buzhou.mcp.McpModule;
import io.github.chyuan_cuihongyuan.buzhou.memory.MemoryModule;
import io.github.chyuan_cuihongyuan.buzhou.skill.SkillModule;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 簇 4 · Skill 体系 + MCP 热插拔（ticket 21 排障 demo）。
 *
 * <p>排障 Agent 的能力供给不能僵化：Skill 随包分发、按需加载；MCP server 清单配置驱动、运行时差量热更。
 * <ul>
 *   <li>{@link #skillCatalogInjectedAndLoadSkillReturnsBody}：classpath Skill 清单注入系统提示，模型用 load_skill 按需取正文。</li>
 *   <li>{@link #mcpToolSetHotSwapAddsAndRemovesTools}：改 ToolSetSpec 清单 → 差量刷新（增 b 删 a、零重启），
 *       下一轮可见工具集变化。用自写的 minimal {@link FakeMcpFactory}（不依赖真实 MCP server 进程）。</li>
 * </ul>
 */
class SkillAndMcpDemoTest {

    @Test
    void skillCatalogInjectedAndLoadSkillReturnsBody() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();

        SkillModule skills = SkillModule.builder().build();
        RuntimeConfig config = RuntimeConfig.merge(
                skills.configure(),
                MemoryModule.configure(Map.of(), stores, model, model, null, skills.catalogRenderer()));
        AgentRuntime runtime = Buzhou.runtime(model, stores, config);

        // 模型先调 load_skill 取正文，再给最终回复
        model.enqueue(AssistantMessage.builder().content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "tc-1", "function", "load_skill", "{\"name\":\"code-review\"}")))
                .build());
        model.enqueue(new AssistantMessage("已加载评审技能，开始审查配置"));
        AgentSession session = runtime.spawn("app", "agent", "sess-skill");
        session.chat("帮我审查这段配置代码");
        session.close();

        // 首轮：Skill 清单注入 system-reminder（只放目录，正文不占预算）
        Prompt first = model.seenPrompts.get(0);
        assertThat(first.getInstructions())
                .anyMatch(m -> ScriptedChatModel.contains(m, "可用技能")
                        && ScriptedChatModel.contains(m, "code-review"));
        // load_skill 工具结果含正文
        Prompt second = model.seenPrompts.get(1);
        assertThat(second.getInstructions())
                .anyMatch(m -> m instanceof ToolResponseMessage
                        && ScriptedChatModel.contains(m, "# Code Review Skill"));
    }

    @Test
    void mcpToolSetHotSwapAddsAndRemovesTools() {
        InMemoryToolSetSpecStore store = new InMemoryToolSetSpecStore();
        store.replaceAll(List.of(spec("metrics-server")));
        FakeMcpFactory factory = new FakeMcpFactory();
        try (McpModule mcp = McpModule.builder()
                .store(store)
                .factory(factory)
                .pollInterval(Duration.ofSeconds(30))   // 依赖 store 写通知而非轮询
                .gracePeriod(Duration.ofMillis(100))
                .build()) {
            McpClientRegistry registry = mcp.registry();

            // 初次建连：metrics-server 可见
            await("metrics connected", 2000, () -> factory.connectCount("metrics-server") == 1);
            assertThat(toolNames(registry)).contains("tool_metrics-server");

            // 运行时改配：摘 metrics-server、增 log-server → 差量刷新（不重启）
            store.replaceAll(List.of(spec("log-server")));
            await("log connected", 2000, () -> factory.connectCount("log-server") == 1);
            await("metrics closed", 2000, () -> factory.closed("metrics-server"));
            assertThat(toolNames(registry)).containsExactly("tool_log-server");
        }
    }

    private static ToolSetSpec spec(String name) {
        return new ToolSetSpec(name, Transport.STDIO, "cmd-" + name, Map.of(), null, null, Set.of());
    }

    private static List<String> toolNames(McpClientRegistry registry) {
        return registry.toolCallbacksFor("app", "agent").stream()
                .map(c -> c.getToolDefinition().name()).toList();
    }

    private static void await(String what, long timeoutMs, BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("interrupted awaiting " + what);
            }
        }
        throw new AssertionError("await 超时: " + what);
    }

    /** 伪 MCP 连接工厂：按 server 名记录建连次数与产物连接（不连真实进程）。 */
    static final class FakeMcpFactory implements McpConnectionFactory {
        final Map<String, AtomicInteger> connectCounts = new ConcurrentHashMap<>();
        final Map<String, List<FakeConnection>> connections = new ConcurrentHashMap<>();

        @Override
        public McpConnection connect(ToolSetSpec spec) {
            connectCounts.computeIfAbsent(spec.name(), k -> new AtomicInteger()).incrementAndGet();
            FakeConnection conn = new FakeConnection(spec.name());
            connections.computeIfAbsent(spec.name(), k -> new CopyOnWriteArrayList<>()).add(conn);
            return conn;
        }

        int connectCount(String server) {
            return connectCounts.getOrDefault(server, new AtomicInteger()).get();
        }

        boolean closed(String server) {
            List<FakeConnection> list = connections.get(server);
            return list != null && list.stream().allMatch(FakeConnection::isClosed);
        }
    }

    /** 伪连接：单工具（名为 tool_&lt;server&gt;），close 标记关闭。 */
    static final class FakeConnection implements McpConnection {
        final String server;
        final ToolCallback tool;
        volatile boolean closed;

        FakeConnection(String server) {
            this.server = server;
            this.tool = TroubleshootingFixture.fixedTool("tool_" + server, "ok:" + server);
        }

        @Override
        public List<ToolCallback> toolCallbacks() {
            return List.of(tool);
        }

        @Override
        public void close() {
            closed = true;
        }

        boolean isClosed() {
            return closed;
        }
    }
}
