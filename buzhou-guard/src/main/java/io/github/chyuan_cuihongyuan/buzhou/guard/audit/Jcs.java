package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * RFC 8785 JSON Canonicalization Scheme（JCS）的自实现子集（wayfinder2 impl-22 / T50，
 * JDK 无内置、零新依赖约 200 行）：
 *
 * <ul>
 *   <li>对象键按 UTF-16 码单元升序（{@link TreeMap} 自然序即满足 JCS 排序规则）；</li>
 *   <li>字符串按 JSON 最小转义（{@code " \ \\ b f n r t} + 其余按 JCS 以裸字符输出）；</li>
 *   <li><b>数值仅整数</b>（JCS 的 ECMAScript number 规范化被审计面约束规避——非法数值抛出）；</li>
 *   <li>数组保序、布尔/null 直出。</li>
 * </ul>
 */
public final class Jcs {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Jcs() {
    }

    /** 任意 Map/List/Scalar → 规范化 JSON 字符串。 */
    public static String canonicalize(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out);
        return out.toString();
    }

    /** JSON 文本 → 规范化 JSON 字符串（重排序/重转义）。 */
    public static String canonicalizeJson(String json) {
        try {
            return canonicalizeNode(MAPPER.readTree(json));
        } catch (Exception e) {
            throw new IllegalArgumentException("JCS 输入不是合法 JSON", e);
        }
    }

    private static String canonicalizeNode(JsonNode node) {
        StringBuilder out = new StringBuilder();
        writeNode(node, out);
        return out.toString();
    }

    private static void write(Object value, StringBuilder out) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String s) {
            writeString(s, out);
        } else if (value instanceof Boolean b) {
            out.append(b);
        } else if (value instanceof Integer || value instanceof Long || value instanceof Short
                || value instanceof java.math.BigInteger) {
            out.append(value);
        } else if (value instanceof Number n) {
            // 诚实子集：浮点/小数不在审计面允许（JCS ECMAScript number 规范化复杂度不值）
            throw new IllegalArgumentException(
                    "JCS 子集仅接受整数数值（收到 " + n + "）；审计记录请用整数或字符串");
        } else if (value instanceof Map<?, ?> map) {
            writeObject(map, out);
        } else if (value instanceof Iterable<?> iterable) {
            out.append('[');
            boolean first = true;
            for (Object item : iterable) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                write(item, out);
            }
            out.append(']');
        } else {
            throw new IllegalArgumentException("JCS 不支持的值类型：" + value.getClass());
        }
    }

    private static void writeObject(Map<?, ?> map, StringBuilder out) {
        // TreeMap 自然序 = 键的 UTF-16 码单元字典序（JCS 排序规则）
        TreeMap<String, Object> sorted = new TreeMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            sorted.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        out.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> entry : sorted.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            writeString(entry.getKey(), out);
            out.append(':');
            write(entry.getValue(), out);
        }
        out.append('}');
    }

    private static void writeNode(JsonNode node, StringBuilder out) {
        if (node == null || node.isNull()) {
            out.append("null");
        } else if (node.isTextual()) {
            writeString(node.asText(), out);
        } else if (node.isBoolean()) {
            out.append(node.asBoolean());
        } else if (node.isIntegralNumber()) {
            out.append(node.asText()); // Jackson 整数文本即规范形式
        } else if (node.isNumber()) {
            throw new IllegalArgumentException("JCS 子集仅接受整数数值（收到 " + node + "）");
        } else if (node.isObject()) {
            out.append('{');
            TreeMap<String, JsonNode> sorted = new TreeMap<>();
            ObjectNode object = (ObjectNode) node;
            for (Iterator<String> it = object.fieldNames(); it.hasNext(); ) {
                String field = it.next();
                sorted.put(field, object.get(field));
            }
            boolean first = true;
            for (Map.Entry<String, JsonNode> entry : sorted.entrySet()) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                writeString(entry.getKey(), out);
                out.append(':');
                writeNode(entry.getValue(), out);
            }
            out.append('}');
        } else if (node.isArray()) {
            out.append('[');
            ArrayNode array = (ArrayNode) node;
            for (int i = 0; i < array.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                writeNode(array.get(i), out);
            }
            out.append(']');
        } else {
            throw new IllegalArgumentException("JCS 不支持的节点类型：" + node.getNodeType());
        }
    }

    /** JCS 字符串序列化：仅转义 JSON 强制字符；控制字符按 JCS 以反斜杠 u 加四位十六进制转义。 */
    private static void writeString(String s, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        // 控制字符按 JCS 转义为 "u" 形式（源码避免字面 unicode 转义序列）
                        out.append('\\').append('u')
                                .append(String.format("%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }
}
