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

    // 错误类别标记（单源：check() 出错文本与统计分桶共用，避免口径漂移——spec 1410）
    private static final String MARK_UNPARSEABLE = "入参不是合法 JSON";
    private static final String MARK_MISSING = "缺少必填字段";
    private static final String MARK_TYPE = "期望 type=";
    private static final String MARK_ENUM = "值不在 enum";
    private static final String MARK_MIN = "小于 minimum";
    private static final String MARK_MAX = "大于 maximum";
    private static final String MARK_LEN_MIN = "长度小于 minLength";
    private static final String MARK_LEN_MAX = "长度大于 maxLength";

    // spec 1410 / T2121：进程级校验读数（静态面——validate 为静态入口，调用点
    // 自动全覆盖；BuzhouMetricsHolder 静态读面先例，resetForTest 为归零注入点）
    private static final java.util.concurrent.atomic.AtomicLong VALIDATIONS =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong ACCEPTED =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong UNPARSEABLE_ARGS =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong MISSING_REQUIRED =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong TYPE_MISMATCH =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong ENUM_VIOLATION =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong RANGE_VIOLATION =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong LENGTH_VIOLATION =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong OTHER_VIOLATION =
            new java.util.concurrent.atomic.AtomicLong();

    private ToolArgsValidator() {
    }

    /** 校验结果分类读数快照（spec 1410 / T2121）。 */
    public record ValidationStats(long validations, long accepted, long rejected,
                                  long unparseableArgs, long missingRequired,
                                  long typeMismatch, long enumViolation,
                                  long rangeViolation, long lengthViolation,
                                  long otherViolation) {

        /** 拒绝总数（= validations − accepted；守恒式见类注）。 */
        public long rejectedDerived() {
            return validations - accepted;
        }
    }

    /** 只读快照：总量守恒 + 七错误桶（桶非互斥，一次校验可含多类错误）。 */
    public static ValidationStats validationStats() {
        return new ValidationStats(VALIDATIONS.get(), ACCEPTED.get(),
                VALIDATIONS.get() - ACCEPTED.get(),
                UNPARSEABLE_ARGS.get(), MISSING_REQUIRED.get(), TYPE_MISMATCH.get(),
                ENUM_VIOLATION.get(), RANGE_VIOLATION.get(), LENGTH_VIOLATION.get(),
                OTHER_VIOLATION.get());
    }

    /** 测试归零口：静态读数的 reset 注入点（BuzhouMetricsHolder 先例）。 */
    public static void resetValidationStatsForTest() {
        VALIDATIONS.set(0);
        ACCEPTED.set(0);
        UNPARSEABLE_ARGS.set(0);
        MISSING_REQUIRED.set(0);
        TYPE_MISMATCH.set(0);
        ENUM_VIOLATION.set(0);
        RANGE_VIOLATION.set(0);
        LENGTH_VIOLATION.set(0);
        OTHER_VIOLATION.set(0);
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
            return Optional.empty(); // 无可校验结构不入账（非一次有效校验）
        }
        JsonNode args = parseOrNull(argumentsJson);
        if (args == null) {
            UNPARSEABLE_ARGS.incrementAndGet();
            VALIDATIONS.incrementAndGet();
            return Optional.of(MARK_UNPARSEABLE
                    + (argumentsJson == null || argumentsJson.isBlank() ? "（空入参）" : ""));
        }
        List<String> errors = new ArrayList<>();
        check(args, schema, "$", errors);
        VALIDATIONS.incrementAndGet();
        if (errors.isEmpty()) {
            ACCEPTED.incrementAndGet();
            return Optional.empty();
        }
        recordRejections(String.join("；", errors));
        return Optional.of(String.join("；", errors));
    }

    /** 错误分桶（标记单源匹配；桶非互斥——一次校验可含多类错误）。 */
    private static void recordRejections(String joined) {
        if (joined.contains(MARK_MISSING)) {
            MISSING_REQUIRED.incrementAndGet();
        }
        if (joined.contains(MARK_TYPE)) {
            TYPE_MISMATCH.incrementAndGet();
        }
        if (joined.contains(MARK_ENUM)) {
            ENUM_VIOLATION.incrementAndGet();
        }
        if (joined.contains(MARK_MIN) || joined.contains(MARK_MAX)) {
            RANGE_VIOLATION.incrementAndGet();
        }
        if (joined.contains(MARK_LEN_MIN) || joined.contains(MARK_LEN_MAX)) {
            LENGTH_VIOLATION.incrementAndGet();
        }
        if (!(joined.contains(MARK_MISSING) || joined.contains(MARK_TYPE)
                || joined.contains(MARK_ENUM) || joined.contains(MARK_MIN)
                || joined.contains(MARK_MAX) || joined.contains(MARK_LEN_MIN)
                || joined.contains(MARK_LEN_MAX))) {
            OTHER_VIOLATION.incrementAndGet();
        }
    }

    private static void check(JsonNode node, JsonNode schema, String path, List<String> errors) {
        // type 关键字
        JsonNode type = schema.path("type");
        if (type.isTextual() && !typeMatches(node, type.asText())) {
            errors.add(path + "：" + MARK_TYPE + type.asText() + "，实际 " + typeName(node));
            return;
        }
        // enum 关键字（文本值或任意 JSON 值比对）
        JsonNode enumNode = schema.path("enum");
        if (enumNode.isArray() && enumNode.size() > 0 && !inEnum(node, enumNode)) {
            errors.add(path + "：" + MARK_ENUM + " 允许范围内（" + enumNode + "）");
        }
        if (node.isObject()) {
            JsonNode properties = schema.path("properties");
            JsonNode required = schema.path("required");
            if (required.isArray()) {
                for (JsonNode req : required) {
                    if (req.isTextual() && node.path(req.asText()).isMissingNode()) {
                        errors.add(path + "：" + MARK_MISSING + "「" + req.asText() + "」");
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
                errors.add(path + "：" + MARK_MIN + "=" + minimum.asDouble());
            }
            if (maximum.isNumber() && node.asDouble() > maximum.asDouble()) {
                errors.add(path + "：" + MARK_MAX + "=" + maximum.asDouble());
            }
        }
        if (node.isTextual()) {
            int len = node.asText().length();
            JsonNode minLength = schema.path("minLength");
            JsonNode maxLength = schema.path("maxLength");
            if (minLength.isNumber() && len < minLength.asInt()) {
                errors.add(path + "：" + MARK_LEN_MIN + "=" + minLength.asInt());
            }
            if (maxLength.isNumber() && len > maxLength.asInt()) {
                errors.add(path + "：" + MARK_LEN_MAX + "=" + maxLength.asInt());
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
