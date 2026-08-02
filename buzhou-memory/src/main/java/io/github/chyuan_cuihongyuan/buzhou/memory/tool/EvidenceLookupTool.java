package io.github.chyuan_cuihongyuan.buzhou.memory.tool;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

public class EvidenceLookupTool implements ToolCallback {

    private final MessageStore messageStore;

    public EvidenceLookupTool(MessageStore messageStore) {
        this.messageStore = messageStore;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name("read_evidence")
                .description("按 evidence-id 回查原始工具返回。可选 offset/limit 做字符区间范围读取。")
                .inputSchema("""
                        {"type":"object","properties":{
                          "evidenceId":{"type":"string","description":"占位符中的 evidence-id"},
                          "offset":{"type":"integer","description":"起始字符偏移，默认 0"},
                          "limit":{"type":"integer","description":"最多返回字符数，默认全文"}
                        },"required":["evidenceId"]}
                        """)
                .build();
    }

    @Override
    public String call(String toolInput) {
        String evidenceId = extract(toolInput, "evidenceId");
        Integer offset = extractInt(toolInput, "offset");
        Integer limit = extractInt(toolInput, "limit");
        return messageStore.findById(evidenceId)
                .map(message -> rangeRead(message, offset, limit))
                .orElse("未找到 evidence-id=" + evidenceId + " 对应的消息");
    }

    private String rangeRead(BuzhouMessage message, Integer offset, Integer limit) {
        String content = message.content() == null ? "" : message.content();
        int start = offset == null ? 0 : Math.max(0, Math.min(offset, content.length()));
        int end = limit == null ? content.length() : Math.min(start + limit, content.length());
        String slice = content.substring(start, end);
        if (end < content.length()) {
            slice += "\n[已截断：" + (content.length() - end) + " 字符未返回，可调整 offset/limit 续读]";
        }
        return slice;
    }

    private String extract(String toolInput, String key) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"")
                .matcher(toolInput);
        return matcher.find() ? matcher.group(1) : "";
    }

    private Integer extractInt(String toolInput, String key) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\"" + key + "\"\\s*:\\s*(\\d+)")
                .matcher(toolInput);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }
}
