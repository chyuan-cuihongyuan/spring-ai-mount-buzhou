package io.github.chyuan_cuihongyuan.buzhou.starter;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.dashboard.DashboardModule;
import io.github.chyuan_cuihongyuan.buzhou.dashboard.config.BuzhouDashboardAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.guard.config.BuzhouGuardAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.mcp.config.BuzhouMcpAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.memory.config.BuzhouMemoryAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.observability.config.BuzhouObservabilityAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.otel.OtelBridge;
import io.github.chyuan_cuihongyuan.buzhou.otel.config.BuzhouOtelAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.skill.config.BuzhouSkillsAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.spill.config.BuzhouSpillAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.store.jdbc.config.BuzhouJdbcStoreAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.store.redis.config.BuzhouRedisStoreAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.tools.config.BuzhouToolsAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * starter 聚合装配的端到端验收（ticket 22 headline）：
 * 全部机制 AutoConfig 共载 → AgentRuntime 默认可用 → spawn 跑通多轮对话；
 * 且 otel / dashboard 默认不装配、store 默认 memory。
 *
 * <p>用 {@link ScriptedChatModel}（随 core test-jar 发布）替代真实模型，避免 API key / 网络。
 */
class BuzhouStarterIntegrationTest {

    @Test
    void starterAssemblesAndRunsMultiTurn() throws Exception {
        Path spillDir = Files.createTempDirectory("buzhou-starter-spill");
        ScriptedChatModel model = new ScriptedChatModel();
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        BuzhouCoreAutoConfiguration.class,
                        BuzhouMemoryAutoConfiguration.class,
                        BuzhouSpillAutoConfiguration.class,
                        BuzhouObservabilityAutoConfiguration.class,
                        BuzhouSkillsAutoConfiguration.class,
                        BuzhouMcpAutoConfiguration.class,
                        BuzhouGuardAutoConfiguration.class,
                        BuzhouToolsAutoConfiguration.class,
                        BuzhouOtelAutoConfiguration.class,
                        BuzhouDashboardAutoConfiguration.class,
                        BuzhouJdbcStoreAutoConfiguration.class,
                        BuzhouRedisStoreAutoConfiguration.class))
                .withBean(ChatModel.class, () -> model)
                .withPropertyValues(
                        "buzhou.spill.root-dir=" + spillDir,
                        "buzhou.spill.sandbox-root=" + spillDir)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(AgentRuntime.class);
                    // otel / dashboard 默认关
                    assertThat(ctx).doesNotHaveBean(OtelBridge.class);
                    assertThat(ctx).doesNotHaveBean(DashboardModule.class);

                    model.enqueueText("第一轮：在的");
                    model.enqueueText("第二轮：继续帮您");
                    AgentRuntime runtime = ctx.getBean(AgentRuntime.class);
                    AgentSession session = runtime.spawn("app", "agent", "sid-starter-1");
                    session.chat("你好");
                    session.chat("继续");
                    session.close();
                    assertThat(model.seenPrompts).hasSizeGreaterThanOrEqualTo(2);
                });
    }
}
