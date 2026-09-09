package io.github.chyuan_cuihongyuan.buzhou.resilience.structured;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 结构化输出契约（spec 402 / T695，instructor 借鉴）——最小子集 JSON
 * schema：required 必备键 + 键→类型表（string/number/integer/boolean/
 * array/object）。完整 JSON Schema 引擎非本轮（诚实边界）；未知声明类型
 * 跳过（宽容）。代码围栏（```json … ```）剥离后解析。
 */
public record OutputSchema(List<String> required, Map<String, String> propertyTypes) {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> KNOWN_TYPES = Set.of(
            "string", "number", "integer", "boolean", "array", "object");

    public OutputSchema {
        required = required == null ? List.of() : List.copyOf(required);
        propertyTypes = propertyTypes == null ? Map.of() : Map.copyOf(propertyTypes);
    }

    /** 支持的声明类型集（装配面校验用）。 */
    public static Set<String> knownTypes() {
        return KNOWN_TYPES;
    }

    /**
     * 验证模型终态文本：空清单 = 合法。解析失败/非 object/缺必备键/类型
     * 不符各成一条错误（人读——直接喂回模型）。
     */
    public List<String> validate(String text) {
        List<String> errors = new ArrayList<>();
        String stripped = stripCodeFence(text);
        JsonNode root;
        try {
            root = MAPPER.readTree(stripped);
        } catch (Exception e) {
            errors.add("输出不是合法 JSON（" + e.getMessage() + "）");
            return errors;
        }
        if (root == null || !root.isObject()) {
            errors.add("输出必须是 JSON 对象，实际是：" + typeName(root));
            return errors;
        }
        for (String key : required) {
            if (!root.has(key) || root.get(key).isNull()) {
                errors.add("缺少必备键「" + key + "」");
            }
        }
        for (Map.Entry<String, String> e : propertyTypes.entrySet()) {
            JsonNode v = root.get(e.getKey());
            if (v == null || v.isNull()) {
                continue; // 缺键由 required 口径管（未声明 required = 可缺省）
            }
            String declared = e.getValue() == null ? "" : e.getValue().toLowerCase(Locale.ROOT);
            if (!KNOWN_TYPES.contains(declared) || matches(declared, v)) {
                continue;
            }
            errors.add("键「" + e.getKey() + "」声明类型 " + declared
                    + "，实际是 " + typeName(v));
        }
        return errors;
    }

    /** 人读契约（反馈消息用）。 */
    public String summary() {
        Map<String, String> types = new LinkedHashMap<>(propertyTypes);
        return "必备键 " + required + (types.isEmpty() ? "" + "" : "；类型 " + types);
    }

    /** 剥离首尾代码围栏（```json … ``` 形态——instructor MD_JSON 同款宽容）。 */
    static String stripCodeFence(String text) {
        if (text == null) {
            return "";
        }
        String t = text.trim();
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            if (firstNewline > 0) {
                t = t.substring(firstNewline + 1);
            }
            int closing = t.lastIndexOf("```");
            if (closing >= 0) {
                t = t.substring(0, closing);
            }
        }
        return t.trim();
    }

    private static boolean matches(String declared, JsonNode v) {
        return switch (declared) {
            case "string" -> v.isTextual();
            case "number" -> v.isNumber();
            case "integer" -> v.isIntegralNumber();
            case "boolean" -> v.isBoolean();
            case "array" -> v.isArray();
            case "object" -> v.isObject();
            default -> true;
        };
    }

    private static String typeName(JsonNode v) {
        if (v == null) {
            return "空";
        }
        if (v.isObject()) {
            return "object";
        }
        if (v.isArray()) {
            return "array";
        }
        if (v.isTextual()) {
            return "string";
        }
        if (v.isIntegralNumber()) {
            return "integer";
        }
        if (v.isNumber()) {
            return "number";
        }
        if (v.isBoolean()) {
            return "boolean";
        }
        if (v.isNull()) {
            return "null";
        }
        return v.getNodeType().toString().toLowerCase(Locale.ROOT);
    }
}
