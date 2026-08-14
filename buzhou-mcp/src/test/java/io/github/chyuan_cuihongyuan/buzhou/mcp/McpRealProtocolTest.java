package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-50 / spec 14 §F：真实协议层集成测试——经真子进程 stdio server 验证
 * {@link SpringAiMcpConnectionFactory} 的握手 / listTools / 工具调用 / 危险工具登记。
 *
 * <p>覆盖此前零测试的工厂生产实现（FakeMcp 只测注册表逻辑）；POSIX 环境专属
 * （java 启动器可用即跨平台，但保守限定非 Windows CI 形态）。
 */
@EnabledOnOs(value = {OS.MAC, OS.LINUX})
class McpRealProtocolTest {

    /**
     * 子进程启动真 MCP stdio server（test classpath 上的 {@link TestMcpStdioServer}），
     * endpoint 传「java -cp <cp> 主类」命令行——工厂按 STDIO 拉起并完成真实 JSON-RPC 握手。
     */
    private static ToolSetSpec stdioServerSpec() {
        // surefire 的 java.class.path 是 booter jar（经典陷阱）；真实测试类路径在本属性
        String classpath = System.getProperty("surefire.test.class.path",
                System.getProperty("java.class.path"));
        String endpoint = "java -cp " + classpath + " "
                + TestMcpStdioServer.class.getName();
        return new ToolSetSpec("stdio-test",
                io.github.chyuan_cuihongyuan.buzhou.core.spi.Transport.STDIO, endpoint, java.util.Map.of(),
                java.time.Duration.ofSeconds(10), java.time.Duration.ofSeconds(10),
                java.util.Set.of());
    }

    @Test
    void handshakeListToolsAndCallToolOverRealProtocol() {
        ToolSetSpec spec = stdioServerSpec();
        McpConnection connection = new SpringAiMcpConnectionFactory().connect(spec);
        try {
            // 工具发现（真实 initialize + tools/list）
            List<org.springframework.ai.tool.ToolCallback> tools = connection.toolCallbacks();
            assertThat(tools).extracting(t -> t.getToolDefinition().name())
                    .containsExactlyInAnyOrder("echo_query", "delete_records");

            // 真实工具调用（tools/call over JSON-RPC stdio）
            org.springframework.ai.tool.ToolCallback echo = tools.stream()
                    .filter(t -> t.getToolDefinition().name().equals("echo_query"))
                    .findFirst().orElseThrow();
            String result = echo.call("{\"query\":\"buzhou-real-protocol\"}");
            assertThat(result).contains("buzhou-real-protocol");
        } finally {
            connection.close();
        }
    }

    /** 危险工具登记：客户端侧动词模式从真实工具快照中筛出 delete_records。 */
    @Test
    void dangerousToolPatternRegistrationFromRealSnapshot() {
        io.github.chyuan_cuihongyuan.buzhou.mcp.internal.DefaultMcpClientRegistry registry =
                new io.github.chyuan_cuihongyuan.buzhou.mcp.internal.DefaultMcpClientRegistry(
                        new SpringAiMcpConnectionFactory(),
                        java.time.Duration.ofSeconds(1), java.time.Duration.ofSeconds(1),
                        null, null, List.of("*delete*", "*.drop*", "*.exec*"));
        try {
            registry.refresh(List.of(stdioServerSpec()));
            assertThat(registry.dangerousToolNames()).containsExactly("delete_records");
            assertThat(registry.activeConnections()).isEqualTo(1);
        } finally {
            registry.shutdown();
        }
    }
}
