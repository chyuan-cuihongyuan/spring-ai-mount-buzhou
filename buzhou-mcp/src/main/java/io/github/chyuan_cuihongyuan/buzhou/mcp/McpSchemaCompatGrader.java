package io.github.chyuan_cuihongyuan.buzhou.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * MCP 工具入参 schema 破坏性变更分级（spec 1402 / T2105 / impl 1055）——
 * buf breaking（protobuf schema 演进的破坏性变更检测）思想：对 tool 目录
 * 轮询到的 inputSchema 前后两版做结构化分级，server 升级是否 break 既有
 * 调用方从「肉眼对 JSON」变成机判。
 *
 * <p><b>判定视角 = 既有调用方守恒</b>：旧 schema 下合法的调用在新 schema 下
 * 仍须合法——新必填属性 / 属性删除 / 类型变更 / 枚举收窄均判 BREAKING；
 * 可选属性新增与枚举放宽为 COMPATIBLE（加法演进）。解析失败 fail-closed
 * 判 BREAKING（与 SsrfGuard 的 fail-closed 同哲学：未知不冒充安全）。
 *
 * <p><b>口径边界</b>：只比顶层 {@code properties}/{@code required}/{@code enum}/
 * {@code type}——嵌套对象结构与数值约束（min/max）方向判定留后续（诚实入档）；
 * 与 {@link McpDirectoryDiff} 互补：那轴显式把入参 schema 划出对比口径（title
 * + 三 hint），本轴是其声明的延伸面。纯函数零 IO，不触 registry/store。
 */
public final class McpSchemaCompatGrader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String PROP_PROPERTIES = "properties";
    private static final String PROP_REQUIRED = "required";
    private static final String PROP_TYPE = "type";
    private static final String PROP_ENUM = "enum";

    /** 兼容性闭集：加法演进 / 破坏性变更。 */
    public enum CompatClass { COMPATIBLE, BREAKING }

    /**
     * @param breaking 是否破坏既有调用方（true 时 reasonList 非空）
     * @param reasons  破坏原因清单（稳定典序）；COMPATIBLE 时为空
     */
    public record SchemaCompatVerdict(CompatClass compatClass, List<String> reasons) {

        static SchemaCompatVerdict compatible() {
            return new SchemaCompatVerdict(CompatClass.COMPATIBLE, List.of());
        }
    }

    /** 分级入口：两版 inputSchema JSON 文本；任一不可解析 → BREAKING（fail-closed）。 */
    public static SchemaCompatVerdict grade(String oldSchemaJson, String newSchemaJson) {
        JsonNode oldSchema;
        JsonNode newSchema;
        try {
            oldSchema = MAPPER.readTree(oldSchemaJson == null ? "" : oldSchemaJson);
            newSchema = MAPPER.readTree(newSchemaJson == null ? "" : newSchemaJson);
        } catch (Exception e) {
            return new SchemaCompatVerdict(CompatClass.BREAKING,
                    List.of("unparseable_schema_fail_closed"));
        }
        JsonNode oldProps = oldSchema.path(PROP_PROPERTIES);
        JsonNode newProps = newSchema.path(PROP_PROPERTIES);
        List<String> reasons = new ArrayList<>();

        // 旧属性逐一检查：删除 / 类型变更 / 新收必填 / 枚举收窄
        if (oldProps.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = oldProps.fields();
            while (fields.hasNext()) {
                String name = fields.next().getKey();
                JsonNode oldProp = oldProps.path(name);
                if (!newProps.has(name)) {
                    reasons.add("removed_property:" + name);
                    continue;
                }
                JsonNode newProp = newProps.path(name);
                String oldType = oldProp.path(PROP_TYPE).asText(null);
                String newType = newProp.path(PROP_TYPE).asText(null);
                if (oldType != null && newType != null && !oldType.equals(newType)) {
                    reasons.add("type_changed:" + name + "(" + oldType + "->" + newType + ")");
                }
                List<String> narrowed = enumNarrowed(oldProp.path(PROP_ENUM),
                        newProp.path(PROP_ENUM));
                for (String value : narrowed) {
                    reasons.add("enum_narrowed:" + name + ":" + value);
                }
            }
        }
        // required 集合扩大的属性（旧可选 → 新必填；含新增即必填的属性）
        TreeSet<String> newlyRequired = new TreeSet<>();
        collectRequired(newSchema, newlyRequired);
        TreeSet<String> previouslyRequired = new TreeSet<>();
        collectRequired(oldSchema, previouslyRequired);
        for (String name : newlyRequired) {
            if (!previouslyRequired.contains(name)) {
                reasons.add("newly_required:" + name);
            }
        }
        // 排序去重，判定稳定
        List<String> stable = reasons.stream().distinct().sorted().toList();
        return stable.isEmpty() ? SchemaCompatVerdict.compatible()
                : new SchemaCompatVerdict(CompatClass.BREAKING, stable);
    }

    private static void collectRequired(JsonNode schema, TreeSet<String> into) {
        JsonNode required = schema.path(PROP_REQUIRED);
        if (required.isArray()) {
            required.forEach(node -> into.add(node.asText()));
        }
    }

    /** 旧枚举是新枚举子集则返回空表；否则返回被移除的枚举值（新枚举缺失 = 不设限，不判窄）。 */
    private static List<String> enumNarrowed(JsonNode oldEnum, JsonNode newEnum) {
        List<String> removed = new ArrayList<>();
        if (!oldEnum.isArray()) {
            return removed;
        }
        if (!newEnum.isArray()) {
            return removed; // 新版删除 enum 约束 = 放宽
        }
        TreeSet<String> newValues = new TreeSet<>();
        newEnum.forEach(node -> newValues.add(node.asText()));
        oldEnum.forEach(node -> {
            String value = node.asText();
            if (!newValues.contains(value)) {
                removed.add(value);
            }
        });
        return removed;
    }
}
