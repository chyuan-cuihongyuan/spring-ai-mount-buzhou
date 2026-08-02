package io.github.chyuan_cuihongyuan.buzhou.spill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

public class ReadRangeTool implements ToolCallback {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SpillService spillService;

    public ReadRangeTool(SpillService spillService) {
        this.spillService = spillService;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name("read_range")
                .description("按 spill:// 路径范围读取已溢出内容。mode=bytes 字符区间 / json JSON path 抽取 / page 数组分页。")
                .inputSchema("""
                        {"type":"object","properties":{
                          "path":{"type":"string","description":"spill:// URI"},
                          "mode":{"type":"string","enum":["bytes","json","page"]},
                          "offset":{"type":"integer","description":"bytes 模式起始偏移"},
                          "limit":{"type":"integer","description":"bytes/page 模式返回量"},
                          "jsonPath":{"type":"string","description":"json 模式路径，如 $.a.b[0]"},
                          "cursor":{"type":"string","description":"page 模式续读游标"}
                        },"required":["path","mode"]}
                        """)
                .build();
    }

    @Override
    public String call(String toolInput) {
        try {
            JsonNode args = MAPPER.readTree(toolInput);
            String path = args.path("path").asText();
            String mode = args.path("mode").asText("bytes");
            RangeReadRequest request = switch (mode) {
                case "json" -> RangeReadRequest.json(args.path("jsonPath").asText("$"));
                case "page" -> RangeReadRequest.page(
                        args.hasNonNull("cursor") ? args.path("cursor").asText() : null,
                        args.hasNonNull("limit") ? args.path("limit").asInt() : 20);
                default -> RangeReadRequest.bytes(
                        args.hasNonNull("offset") ? args.path("offset").asInt() : 0,
                        args.hasNonNull("limit") ? args.path("limit").asInt() : 20000);
            };
            RangeReadResult result = spillService.readBack(path, request);
            return result.truncated()
                    ? result.content() + "\n[已截断，可用 offset/cursor 续读]"
                    : result.content();
        } catch (Exception e) {
            return "read_range 调用失败：" + e.getMessage();
        }
    }
}
