package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * impl-50 / spec 14 §F：真实 MCP stdio server（测试子进程入口）。
 *
 * <p>经 {@code java -cp <test-classpath> ...TestMcpStdioServer} 启动，在 stdin/stdout 上
 * 讲真正的 MCP 协议（initialize 握手 → tools/list → tools/call）。
 * 注册两个工具：echo_query（无害）与 delete_records（危险名——供客户端侧模式登记断言）。
 */
public final class TestMcpStdioServer {

    private TestMcpStdioServer() {
    }

    public static void main(String[] args) throws InterruptedException {
        // stdio server 的 stdout 是协议通道——MCP server 日志（SLF4J/logback 默认打 stdout）
        // 会污染 JSON-RPC 流（客户端把日志行当 JSON 解析即炸）。启动前静音全部日志。
        try {
            ch.qos.logback.classic.LoggerContext logback =
                    (ch.qos.logback.classic.LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
            logback.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME)
                    .setLevel(ch.qos.logback.classic.Level.OFF);
        } catch (Throwable ignored) {
            // 非 logback 绑定（或无 slf4j）：无日志可静音
        }
        JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(
                tools.jackson.databind.json.JsonMapper.builder().build());
        McpServer.sync(new StdioServerTransportProvider(jsonMapper))
                .serverInfo(McpSchema.Implementation.builder("test-mcp-server", "1.0.0").build())
                .tools(
                        McpServerFeatures.SyncToolSpecification.builder()
                                .tool(McpSchema.Tool.builder("echo_query")
                                        .description("回显 query 参数")
                                        .inputSchema(Map.of(
                                                "type", "object",
                                                "properties", Map.of(
                                                        "query", Map.of("type", "string")),
                                                "required", java.util.List.of("query")))
                                        .build())
                                .callHandler((exchange, request) -> {
                                    Object query = request.arguments().get("query");
                                    return McpSchema.CallToolResult.builder()
                                            .addTextContent(String.valueOf(query))
                                            .build();
                                })
                                .build(),
                        McpServerFeatures.SyncToolSpecification.builder()
                                .tool(McpSchema.Tool.builder("delete_records")
                                        .description("删除记录（危险工具，供 HITL 登记断言）")
                                        .inputSchema(Map.of(
                                                "type", "object",
                                                "properties", Map.of(
                                                        "table", Map.of("type", "string")),
                                                "required", java.util.List.of("table")))
                                        .build())
                                .callHandler((exchange, request) -> McpSchema.CallToolResult.builder()
                                        .addTextContent("deleted").build())
                                .build())
                .build();
        // stdio server 由传输线程驱动；主线程保活直到进程被 client 关闭
        new CountDownLatch(1).await();
    }
}
