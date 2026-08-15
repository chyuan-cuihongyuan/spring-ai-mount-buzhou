package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import com.sun.net.httpserver.HttpServer;
import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionMigrator;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.core.webhook.BuzhouWebhookProperties;
import io.github.chyuan_cuihongyuan.buzhou.core.webhook.WebhookEventForwarder;
import io.github.chyuan_cuihongyuan.buzhou.skill.SkillModule;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * effort #8 新能力演示（T145 / impl-118）：skill_search 两步发现、死信一键重放运维、
 * H2→内存迁移续用——examples 接缝文档（用户可读场景）。
 */
class Effort8CapabilitiesDemoTest {

    /** 1) 技能发现：目录截断场景下模型经 skill_search 找到并 load 隐藏技能。 */
    @Test
    void skillSearchDiscoversHiddenSkill() {
        ScriptedChatModel model = new ScriptedChatModel();
        SkillModule skills = SkillModule.builder().build();
        // 两步脚本：先检索（发现 sql-tuning）→ 再加载正文
        model.enqueue(AssistantMessage.builder().content("").toolCalls(List.of(
                new AssistantMessage.ToolCall("tc-1", "function", "skill_search",
                        "{\"query\":\"sql\"}"))).build());
        model.enqueue(AssistantMessage.builder().content("").toolCalls(List.of(
                new AssistantMessage.ToolCall("tc-2", "function", "load_skill",
                        "{\"name\":\"sql-tuning\"}"))).build());
        model.enqueueText("已加载慢 SQL 诊断技能");
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(),
                skills.configure());
        AgentSession session = runtime.spawn("app", "demo", "search-demo");

        assertThat(session.chat("我需要优化慢查询")).contains("已加载");

        // 第一步模型看到检索结果（ToolResponse 含 sql-tuning 条目）；第二步 load 成功（非「不存在」）
        String step1 = model.seenPrompts.get(1).getInstructions().stream()
                .filter(m -> m instanceof org.springframework.ai.chat.messages.ToolResponseMessage)
                .map(m -> ((org.springframework.ai.chat.messages.ToolResponseMessage) m)
                        .getResponses().toString())
                .findFirst().orElse("");
        String step2 = model.seenPrompts.get(2).getInstructions().stream()
                .filter(m -> m instanceof org.springframework.ai.chat.messages.ToolResponseMessage)
                .map(m -> ((org.springframework.ai.chat.messages.ToolResponseMessage) m)
                        .getResponses().toString())
                .findFirst().orElse("");
        assertThat(step1).contains("sql-tuning");
        assertThat(step2).doesNotContain("技能不存在"); // load 成功
        session.close();
    }

    /** 2) 死信运维：恒 500 耗尽 → 修好端点 → replayDeadLetters 一键恢复。 */
    @Test
    void deadLetterOpsReplayAfterFix() throws Exception {
        ConcurrentLinkedQueue<String> received = new ConcurrentLinkedQueue<>();
        AtomicInteger[] status = {new AtomicInteger(500)};
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", exchange -> {
            exchange.getRequestBody().readAllBytes();
            if (status[0].get() == 200) {
                received.add("ok");
            }
            exchange.sendResponseHeaders(status[0].get(), -1);
            exchange.close();
        });
        server.start();
        InMemorySessionStateStore shared = new InMemorySessionStateStore();
        WebhookEventForwarder forwarder = new WebhookEventForwarder(new BuzhouWebhookProperties(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/hook",
                null, Duration.ofSeconds(2), 2, 100, null), shared);
        forwarder.onEvent(io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent.of(
                "order.completed", Map.of("orderId", "P-9527")));
        await(() -> forwarder.deadLettered() == 1); // 端点故障期事件死信

        status[0].set(200); // 运维修复端点
        int replayed = forwarder.replayDeadLetters(); // 一键重放

        assertThat(replayed).isEqualTo(1);
        await(() -> forwarder.delivered() == 1);
        assertThat(received).hasSize(1); // 消费端终见
        forwarder.close();
        server.stop(0);
    }

    /** 3) 迁移演练：JDBC(H2) 缩容下线 → 会话迁内存续用。 */
    @Test
    void migratesFromJdbcOnDecommission() {
        org.h2.jdbcx.JdbcDataSource h2 = new org.h2.jdbcx.JdbcDataSource();
        h2.setURL("jdbc:h2:mem:demo-mig-" + java.util.UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        BuzhouStores jdbcStores = io.github.chyuan_cuihongyuan.buzhou.store.jdbc.JdbcBuzhouStores
                .createWithRecovery(h2, io.github.chyuan_cuihongyuan.buzhou.store.jdbc.Dialect.H2)
                .stores();
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("jdbc 期答复");
        model.enqueueText("迁移后续聊");
        AgentRuntime jdbc = Buzhou.runtime(model, jdbcStores, RuntimeConfig.defaults());
        AgentSession session = jdbc.spawn("app", "ag", "decommission-1");
        session.chat("业务上下文");
        session.close();

        AgentRuntime memory = Buzhou.runtime(model, Buzhou.inMemoryStores(), RuntimeConfig.defaults());
        String migratedId = SessionMigrator.migrate(jdbc, memory, "decommission-1", false);

        AgentSession resumed = memory.spawn("app", "ag", migratedId);
        assertThat(resumed.chat("继续")).isEqualTo("迁移后续聊");
        assertThat(model.seenPrompts.get(1).getContents().toString()).contains("业务上下文");
        resumed.close();
    }

    private static void await(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("等待超时");
            }
            Thread.sleep(20);
        }
    }
}
