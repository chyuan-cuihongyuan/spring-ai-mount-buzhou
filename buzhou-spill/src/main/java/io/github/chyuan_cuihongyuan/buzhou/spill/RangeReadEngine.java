package io.github.chyuan_cuihongyuan.buzhou.spill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Base64;
import java.util.Iterator;

public final class RangeReadEngine {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RangeReadEngine() {
    }

    public static RangeReadResult read(String content, RangeReadRequest request) {
        return switch (request.mode()) {
            case BYTES -> readBytes(content, request);
            case JSON -> readJson(content, request);
            case PAGE -> readPage(content, request);
        };
    }

    private static RangeReadResult readBytes(String content, RangeReadRequest request) {
        int offset = request.offset() == null ? 0 : Math.max(0, request.offset());
        int limit = request.limit() == null ? 20000 : request.limit();
        int start = Math.min(offset, content.length());
        int end = Math.min(start + limit, content.length());
        return new RangeReadResult(content.substring(start, end), content.length(),
                end < content.length(), null);
    }

    private static RangeReadResult readJson(String content, RangeReadRequest request) {
        try {
            JsonNode node = MAPPER.readTree(content);
            JsonNode selected = evaluatePath(node, request.jsonPath());
            String rendered = selected == null ? "null"
                    : (selected.isTextual() ? selected.asText() : MAPPER.writeValueAsString(selected));
            return new RangeReadResult(rendered, content.length(), false, null);
        } catch (Exception e) {
            return new RangeReadResult("JSON path 求值失败：" + e.getMessage(),
                    content.length(), false, null);
        }
    }

    static JsonNode evaluatePath(JsonNode node, String path) {
        if (path == null || path.isBlank() || path.equals("$")) {
            return node;
        }
        String normalized = path.startsWith("$.") ? path.substring(2)
                : (path.startsWith("$") ? path.substring(1) : path);
        JsonNode current = node;
        for (String segment : normalized.split("\\.")) {
            if (segment.isEmpty()) {
                continue;
            }
            String field = segment;
            while (true) {
                int bracket = field.indexOf('[');
                if (bracket < 0) {
                    if (!field.isEmpty()) {
                        current = current.path(field);
                    }
                    break;
                }
                String head = field.substring(0, bracket);
                if (!head.isEmpty()) {
                    current = current.path(head);
                }
                int close = field.indexOf(']', bracket);
                int index = Integer.parseInt(field.substring(bracket + 1, close));
                current = current.path(index);
                field = field.substring(close + 1);
            }
        }
        return current;
    }

    private static RangeReadResult readPage(String content, RangeReadRequest request) {
        try {
            JsonNode node = MAPPER.readTree(content);
            if (!node.isArray()) {
                return new RangeReadResult("page 模式仅支持 JSON 数组内容",
                        content.length(), false, null);
            }
            ArrayNode array = (ArrayNode) node;
            int offset = request.cursor() == null ? 0 : decodeCursor(request.cursor());
            int limit = request.limit() == null ? 20 : request.limit();
            ObjectNode result = MAPPER.createObjectNode();
            ArrayNode items = MAPPER.createArrayNode();
            int end = Math.min(offset + limit, array.size());
            for (int i = offset; i < end; i++) {
                items.add(array.get(i));
            }
            result.set("items", items);
            result.put("totalCount", array.size());
            boolean truncated = end < array.size();
            result.put("truncated", truncated);
            if (truncated) {
                result.put("nextCursor", encodeCursor(end));
            }
            return new RangeReadResult(MAPPER.writeValueAsString(result), content.length(),
                    truncated, truncated ? encodeCursor(end) : null);
        } catch (Exception e) {
            return new RangeReadResult("分页读取失败：" + e.getMessage(), content.length(), false, null);
        }
    }

    public static String previewOf(String content, int previewChars, int listPreviewItems) {
        String trimmed = content.strip();
        if (trimmed.startsWith("[")) {
            try {
                JsonNode node = MAPPER.readTree(trimmed);
                if (node.isArray()) {
                    RangeReadResult page = readPage(content,
                            RangeReadRequest.page(null, listPreviewItems));
                    return page.content();
                }
            } catch (Exception ignored) {
            }
        }
        return content.substring(0, Math.min(previewChars, content.length()));
    }

    private static String encodeCursor(int offset) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(String.valueOf(offset).getBytes());
    }

    private static int decodeCursor(String cursor) {
        return Integer.parseInt(new String(Base64.getUrlDecoder().decode(cursor)));
    }
}
