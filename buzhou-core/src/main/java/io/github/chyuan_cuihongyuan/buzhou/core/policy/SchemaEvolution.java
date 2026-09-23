package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Avro 读写模式解析（spec 4045 / T6091 / impl 2146）——
 * Apache Avro Schema Resolution 思想：写方 schema 的数据被
 * 读方 schema 读取时按确定性规则解析——字段按名（含 alias
 * 别名）配对；类型按**加宽白名单**（int→long/float/double、
 * long→float/double、float→double，Avro widening 同表）；
 * 读方缺字段 → 默认值补位（usesDefault 显形）；写方多余字段
 * → dropped 读数（数据被诚实丢弃可见）；不兼容 fail-fast
 * 带字段名。自由漂移（语义静默劣化）与无规则加字段即崩的
 * 病解；{@link Plan} 确定性可回放。
 *
 * <p>record+原语口径（union/enum/array 不在本件）。
 */
public final class SchemaEvolution {

    /** 字段原语类型（加宽链锚点）。 */
    public enum FieldType {
        STRING, BOOLEAN, INT, LONG, FLOAT, DOUBLE;

        /** 加宽兼容（writeType → 本型是否 Avro widening 可达）。 */
        boolean acceptsWideningFrom(FieldType writeType) {
            return switch (this) {
                case LONG -> writeType == INT;
                case FLOAT -> writeType == INT || writeType == LONG;
                case DOUBLE -> writeType == INT || writeType == LONG || writeType == FLOAT;
                default -> false;
            };
        }
    }

    /**
     * 模式字段。
     *
     * @param name 字段名（读方匹配键；写方被配对键）
     * @param type 字段类型
     * @param aliases 别名表（读方字段的补充匹配键）
     * @param hasDefault 是否声明默认值（读方缺字段补位依据）
     */
    public record Field(String name, FieldType type, List<String> aliases, boolean hasDefault) {

        /** 无别名字段。 */
        public static Field of(String name, FieldType type, boolean hasDefault) {
            return new Field(name, type, List.of(), hasDefault);
        }
    }

    /**
     * 模式（record 简形）。
     *
     * @param name 模式名
     * @param fields 字段集（序即声明序）
     */
    public record Schema(String name, List<Field> fields) {

        /** 定构校验（null/空名 fail-fast）。 */
        public Schema {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("schema 名非空");
            }
            fields = List.copyOf(fields);
        }
    }

    /**
     * 逐字段解析项。
     *
     * @param readerField 读方字段名
     * @param writerField 写方来源字段名
     * @param readType 读方类型
     * @param writeType 写方类型
     * @param promoted 是否加宽转换
     * @param usesDefault 是否默认值补位（写方无此字段）
     */
    public record FieldPlan(String readerField, String writerField, FieldType readType,
            FieldType writeType, boolean promoted, boolean usesDefault) {
    }

    /**
     * 解析计划。
     *
     * @param fields 逐字段计划（读方声明序）
     * @param dropped 写方多余字段（数据被诚实丢弃可见）
     */
    public record Plan(List<FieldPlan> fields, List<String> dropped) {
    }

    private SchemaEvolution() {
    }

    /** 读写模式解析（不兼容 fail-fast 带字段名）。 */
    public static Plan resolve(Schema writer, Schema reader) {
        if (writer == null || reader == null) {
            throw new IllegalArgumentException("读写 schema 非 null");
        }
        Map<String, Field> writerByName = new LinkedHashMap<>();
        for (Field field : writer.fields()) {
            writerByName.put(field.name(), field);
        }
        List<FieldPlan> plans = new ArrayList<>(reader.fields().size());
        List<String> dropped = new ArrayList<>();
        for (Field readerField : reader.fields()) {
            Field writerField = matchWriterField(writerByName, readerField);
            if (writerField == null) {
                if (!readerField.hasDefault()) {
                    throw new IllegalArgumentException("读方字段无写方来源且无默认值：" + readerField.name());
                }
                plans.add(new FieldPlan(readerField.name(), null, readerField.type(), null, false, true));
                continue;
            }
            if (writerField.type() != readerField.type()
                    && !readerField.type().acceptsWideningFrom(writerField.type())) {
                throw new IllegalArgumentException("类型不兼容（" + writerField.type() + " → "
                        + readerField.type() + "）：" + readerField.name());
            }
            boolean promoted = writerField.type() != readerField.type();
            plans.add(new FieldPlan(readerField.name(), writerField.name(),
                    readerField.type(), writerField.type(), promoted, false));
        }
        for (Field writerField : writer.fields()) {
            if (plans.stream().noneMatch(plan -> writerField.name().equals(plan.writerField()))) {
                dropped.add(writerField.name());
            }
        }
        return new Plan(List.copyOf(plans), List.copyOf(dropped));
    }

    /** 名匹配，未中再扫别名表（读方字段的补充匹配键）。 */
    private static Field matchWriterField(Map<String, Field> writerByName, Field readerField) {
        Field direct = writerByName.get(readerField.name());
        if (direct != null) {
            return direct;
        }
        for (String alias : readerField.aliases()) {
            Field aliased = writerByName.get(alias);
            if (aliased != null) {
                return aliased;
            }
        }
        return null;
    }
}
