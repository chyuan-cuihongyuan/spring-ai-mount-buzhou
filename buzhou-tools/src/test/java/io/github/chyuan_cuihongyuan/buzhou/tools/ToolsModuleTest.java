package io.github.chyuan_cuihongyuan.buzhou.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ToolsModule 装配（ticket 16 验收：默认开关矩阵——危险工具默认不出现在工具清单；
 * 每个工具参数 Schema 与 06 spec 一致）。
 */
class ToolsModuleTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SessionStateStore stateStore = new InMemorySessionStateStore();

    private static List<String> toolNames(RuntimeConfig config) {
        return config.autoTools().stream().map(t -> t.getToolDefinition().name()).toList();
    }

    @Test
    void defaultMatrixExcludesDangerousTools() {
        ToolsModule module = ToolsModule.builder(stateStore).build();
        RuntimeConfig config = module.configure();

        assertThat(toolNames(config)).containsExactlyInAnyOrder("read_file", "todo");
        assertThat(toolNames(config)).noneMatch(n -> n.startsWith("write_file")
                || n.equals("run_command") || n.equals("http_request"));
        assertThat(module.enabledDangerousToolNames()).isEmpty();
    }

    @Test
    void optInAddsDangerousToolsAndGuardNames() {
        ToolsModule module = ToolsModule.builder(stateStore)
                .writeFileEnabled(true)
                .runCommandEnabled(true)
                .httpRequestEnabled(true)
                .build();
        RuntimeConfig config = module.configure();

        assertThat(toolNames(config)).containsExactlyInAnyOrder(
                "read_file", "todo", "write_file", "run_command", "http_request");
        // opt-in 后挂 HITL 守卫的名单暴露给装配侧
        assertThat(module.enabledDangerousToolNames())
                .containsExactlyInAnyOrder("write_file", "run_command", "http_request");
        // 写侧长内容参数声明（供 SpillGuardModule 接线）
        assertThat(module.longContentParamDecls()).containsExactlyInAnyOrder(
                new LongContentParamDecl("write_file", "content", "contentPath"),
                new LongContentParamDecl("http_request", "body", "bodyPath"));
    }

    @Test
    void buzhouToolMetadataFlowsIntoRuntimeConfig() {
        RuntimeConfig config = ToolsModule.builder(stateStore)
                .writeFileEnabled(true).build().configure();
        assertThat(config.idempotentToolNames()).containsExactly("read_file");
        assertThat(config.serialGroups()).containsEntry("write_file", "file");
    }

    @Test
    void fromYmlParsesSwitchMatrixAndSafetyConfig() {
        ToolsModule module = ToolsModule.fromYml(stateStore, Map.of(
                "enabled", true,
                "write-file", Map.of("enabled", true),
                "run-command", Map.of("enabled", true, "timeout-seconds", 45,
                        "blacklist", List.of("sudo*")),
                "http-request", Map.of("enabled", true,
                        "ssrf", Map.of("block-private-ranges", true,
                                "allowlist", List.of("api.partner.example"))),
                "file-sandbox", Map.of("root", "/tmp", "allowed-paths", List.of("/var/data"))))
                .build();
        assertThat(toolNames(module.configure())).contains(
                "write_file", "run_command", "http_request");
    }

    @Test
    void disabledModuleYieldsNoTools() {
        assertThat(ToolsModule.builder(stateStore).enabled(false).build().configure().autoTools())
                .isEmpty();
    }

    /** 参数 Schema 与 spec 06 逐工具一致（验收：瘦 Schema + 参数名/必填对齐）。 */
    @Test
    void schemasMatchSpec() throws Exception {
        ToolsModule module = ToolsModule.builder(stateStore)
                .writeFileEnabled(true).runCommandEnabled(true).httpRequestEnabled(true).build();
        Map<String, ToolCallback> byName = new java.util.HashMap<>();
        module.configure().autoTools().forEach(t -> byName.put(t.getToolDefinition().name(), t));

        assertSchema(byName.get("read_file"), List.of("path"), List.of("path"));
        assertSchema(byName.get("todo"), List.of("action", "items", "ids"), List.of("action"));
        assertSchema(byName.get("write_file"), List.of("path", "content", "contentPath"),
                List.of("path"));
        assertSchema(byName.get("run_command"), List.of("command", "workdir", "timeoutSeconds"),
                List.of("command"));
        assertSchema(byName.get("http_request"),
                List.of("method", "url", "headers", "body", "bodyPath", "timeoutSeconds"),
                List.of("method", "url"));
    }

    private static void assertSchema(ToolCallback tool, List<String> expectedProps,
                                     List<String> expectedRequired) throws Exception {
        assertThat(tool).isNotNull();
        JsonNode schema = MAPPER.readTree(tool.getToolDefinition().inputSchema());
        assertThat(schema.path("properties").properties().stream().map(Map.Entry::getKey).toList())
                .containsExactlyInAnyOrderElementsOf(expectedProps);
        List<String> required = new java.util.ArrayList<>();
        schema.path("required").forEach(n -> required.add(n.asText()));
        assertThat(required).containsExactlyInAnyOrderElementsOf(expectedRequired);
    }
}
