package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 工具 schema 健康审计（spec 1435 / T2165 替位编号见台账：R36 = effort #1435 /
 * 票 T2173 + T2174 / impl 1088）——ajv / OpenAPI schema 校验思想：工具入参
 * schema 的健康度决定 {@code ToolArgsValidator} 是否实际生效——**schema 缺失
 * 或不可解析时校验器 permissive 放行**（spec 12 依赖卫生决策：宁可漏报校验、
 * 绝不误拦），但「多少工具在裸奔」无审计面。本审计把 schema 状态分桶显形：
 * VALID（可解析且 object 型）/ MISSING（null/空白）/ UNPARSEABLE（非法 JSON）/
 * NOT_OBJECT（可解析但非 object 型——校验器同样跳过）。
 *
 * <p>纯函数零状态：吃 ToolCallback 清单（调用方从装配/注册表抽取）；
 * findings 有界封顶（基数纪律），审计不裁决（修复归宿主装配）。
 */
public final class ToolSchemaHealthAudit {

    /** findings 榜容量（基数纪律）。 */
    static final int FINDINGS_CAPACITY = 16;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** schema 状态闭集。 */
    public enum SchemaState { VALID, MISSING, UNPARSEABLE, NOT_OBJECT }

    /**
     * @param toolName 工具名
     * @param state    schema 状态
     */
    public record SchemaFinding(String toolName, SchemaState state) {
    }

    /**
     * @param totalTools    工具总数
     * @param valid         VALID 计数（可解析且 type=object）
     * @param missing       MISSING 计数（schema null/空白）
     * @param unparseable   UNPARSEABLE 计数（非法 JSON）
     * @param notObject     NOT_OBJECT 计数（可解析但非 object 型）
     * @param findings      非 VALID 明细（有界封顶）
     */
    public record Report(int totalTools, int valid, int missing, int unparseable,
                         int notObject, List<SchemaFinding> findings) {

        /** 裸奔率 = 非 VALID 占比（0 工具哨兵 -1）。 */
        public double bypassRatio() {
            return totalTools == 0 ? -1d
                    : (double) (missing + unparseable + notObject) / totalTools;
        }
    }

    /** 审计入口：工具回调清单。 */
    public static Report analyze(List<org.springframework.ai.tool.ToolCallback> callbacks) {
        int valid = 0;
        int missing = 0;
        int unparseable = 0;
        int notObject = 0;
        List<SchemaFinding> findings = new java.util.ArrayList<>();
        for (org.springframework.ai.tool.ToolCallback callback : callbacks) {
            String name = callback.getToolDefinition().name();
            String schema = callback.getToolDefinition().inputSchema();
            SchemaState state = classify(schema);
            switch (state) {
                case VALID -> valid++;
                case MISSING -> missing++;
                case UNPARSEABLE -> unparseable++;
                case NOT_OBJECT -> notObject++;
            }
            if (state != SchemaState.VALID && findings.size() < FINDINGS_CAPACITY) {
                findings.add(new SchemaFinding(name, state));
            }
        }
        return new Report(callbacks.size(), valid, missing, unparseable,
                notObject, List.copyOf(findings));
    }

    private static SchemaState classify(String schema) {
        if (schema == null || schema.isBlank()) {
            return SchemaState.MISSING;
        }
        JsonNode node;
        try {
            node = MAPPER.readTree(schema);
        } catch (Exception e) {
            return SchemaState.UNPARSEABLE;
        }
        // 与 ToolArgsValidator.validate 的跳过条件严格同口径：
        // 非 object、或 properties/required/type 全缺 → 校验器 permissive 跳过 = 裸奔
        if (!node.isObject()) {
            return SchemaState.NOT_OBJECT;
        }
        boolean hasProperties = !node.path("properties").isMissingNode();
        boolean hasRequired = !node.path("required").isMissingNode();
        boolean hasType = !node.path("type").isMissingNode();
        return (hasProperties || hasRequired || hasType)
                ? SchemaState.VALID : SchemaState.NOT_OBJECT;
    }
}
