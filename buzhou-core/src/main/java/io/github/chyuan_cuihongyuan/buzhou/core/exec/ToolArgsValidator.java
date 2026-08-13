package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * 工具入参 JSON Schema 结构校验器（wayfinder2 impl-04 / T30 / docs/spec/12）：
 * 工具执行<b>前</b>校验 arguments，未过 schema 的调用不执行、直接回喂校验错误（REASK）。
 *
 * <p><b>自实现的最小结构子集</b>（零新依赖，遵守 spec 12 依赖卫生）：覆盖 Spring AI 工具
 * schema 生成面实际使用的关键字——{@code type}/{@code required}/{@code properties}/
 * {@code items}/{@code enum}/{@code minLength}/{@code maxLength}/{@code minimum}/{@code maximum}；
 * <b>未知关键字一律忽略</b>（permissive），schema 缺失/空/不可解析时放行——宁可漏报校验、
 * 绝不误拦合法调用。
 *
 * <p>来源：Pydantic AI（ValidationError 自动转 retry 消息、默认 retries=1）+ Instructor
 * （max_retries + REASK）的 best-of-breed 思想。
 */
public final class ToolArgsValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> SUPPORTED_TYPES =
            Set.of("object", "array", "string", "integer", "number", "boolean", "null");

    private ToolArgsValidator() {
    }

    /**
     * 校验入参是否符合 schema。
     *
     * @return {@link Optional#empty()} = 通过（或 schema 不具备可校验结构）；否则为失败描述。
     */
    public static Optional<String> validate(String schemaJson, String argumentsJson) {
        JsonNode schema = parseOrNull(schemaJson);
        if (schema == null || !schema.isObject() || schema.path("properties").isMissingNode()
                && schema.path("required").isMissingNode() && schema.path("type").isMissingNode()) {
            return Optional.empty();
        }
        JsonNode args = parseOrNull(argumentsJson);
        if (args == null) {
            return Optional.of("入参不是合法 JSON"
                    + (argumentsJson == null || argumentsJson.isBlank() ? "（空入参）" : ""));
        }
        List<String> errors = new ArrayList<>();
        check(args, schema, "$", errors);
        if (errors.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(String.join("；", errors));
    }

    private static void check(JsonNode node, JsonNode schema, String path, List<String> errors) {
        // type 关键字
        JsonNode type = schema.path("type");
        if (type.isTextual() && !typeMatches(node, type.asText())) {
            errors.add(path + "：期望 type=" + type.asText() + "，实际 " + typeName(node));
            return;
        }
        // enum 关键字（文本值或任意 JSON 值比对）
        JsonNode enumNode = schema.path("enum");
        if (enumNode.isArray() && enumNode.size() > 0 && !inEnum(node, enumNode)) {
            errors.add(path + "：值不在 enum 允许范围内（" + enumNode + "）");
        }
        if (node.isObject()) {
            JsonNode properties = schema.path("properties");
            JsonNode required = schema.path("required");
            if (required.isArray()) {
                for (JsonNode req : required) {
                    if (req.isTextual() && node.path(req.asText()).isMissingNode()) {
                        errors.add(path + "：缺少必填字段「" + req.asText() + "」");
                    }
                }
            }
            if (properties.isObject()) {
                for (Iterator<String> it = properties.fieldNames(); it.hasNext(); ) {
                    String field = it.next();
                    JsonNode child = node.path(field);
                    if (!child.isMissingNode()) {
                        check(child, properties.path(field), path + "." + field, errors);
                    }
                }
            }
        }
        if (node.isArray()) {
            JsonNode items = schema.path("items");
            if (items.isObject()) {
                for (int i = 0; i < node.size(); i++) {
                    check(node.get(i), items, path + "[" + i + "]", errors);
                }
            }
        }
        // 数值与字符串边界
        if (node.isNumber()) {
            JsonNode minimum = schema.path("minimum");
            JsonNode maximum = schema.path("maximum");
            if (minimum.isNumber() && node.asDouble() < minimum.asDouble()) {
                errors.add(path + "：小于 minimum=" + minimum.asDouble());
            }
            if (maximum.isNumber() && node.asDouble() > maximum.asDouble()) {
                errors.add(path + "：大于 maximum=" + maximum.asDouble());
            }
        }
        if (node.isTextual()) {
            int len = node.asText().length();
            JsonNode minLength = schema.path("minLength");
            JsonNode maxLength = schema.path("maxLength");
            if (minLength.isNumber() && len < minLength.asInt()) {
                errors.add(path + "：长度小于 minLength=" + minLength.asInt());
            }
            if (maxLength.isNumber() && len > maxLength.asInt()) {
                errors.add(path + "：长度大于 maxLength=" + maxLength.asInt());
            }
        }
    }

    private static boolean typeMatches(JsonNode node, String expected) {
        return switch (expected) {
            case "object" -> node.isObject();
            case "array" -> node.isArray();
            case "string" -> node.isTextual();
            case "integer" -> node.isIntegralNumber();
            case "number" -> node.isNumber();
            case "boolean" -> node.isBoolean();
            case "null" -> node.isNull();
            default -> true; // 未知类型名按匹配处理（permissive）
        };
    }

    private static boolean inEnum(JsonNode node, JsonNode enumNode) {
        for (JsonNode allowed : enumNode) {
            if (allowed.equals(node)) {
                return true;
            }
            if (node.isTextual() && allowed.isTextual() && node.asText().equals(allowed.asText())) {
                return true;
            }
        }
        return false;
    }

    private static String typeName(JsonNode node) {
        if (node.isObject()) {
            return "object";
        }
        if (node.isArray()) {
            return "array";
        }
        if (node.isTextual()) {
            return "string";
        }
        if (node.isIntegralNumber()) {
            return "integer";
        }
        if (node.isNumber()) {
            return "number";
        }
        if (node.isBoolean()) {
            return "boolean";
        }
        if (node.isNull()) {
            return "null";
        }
        return node.getNodeType().toString().toLowerCase(Locale.ROOT);
    }

    private static JsonNode parseOrNull(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }
}
