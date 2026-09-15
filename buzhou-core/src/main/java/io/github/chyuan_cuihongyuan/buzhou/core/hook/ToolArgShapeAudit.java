package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 工具参数形态分布审计（L 会话 1700 系 R14 = effort #1713 / spec 1713 /
 * 票 T2627 + T2628 / impl 1313）——jq 类型分诊 / fail2ban 模式分类思想：
 * 工具入参是结构化 JSON、裸数值还是巨型 blob，决定下游限幅/裁剪/脱敏的
 * 策略选择——形态分布显形，策略才有依据。
 *
 * <p>实例面线程安全：`record(String)` 逐参分类计数；分类闭集
 * {@link Shape}（EMPTY/JSON_OBJECT/JSON_ARRAY/NUMERIC/BOOLEAN/
 * LARGE_BLOB/PLAIN_TEXT）；超长阈值默认 4096 字符可调。纯读面 opt-in。
 *
 * @since 1.0.0
 */
public final class ToolArgShapeAudit {

    /** 默认大负载阈值（字符）。 */
    public static final int DEFAULT_LARGE_BLOB_THRESHOLD = 4096;

    /** 参数形态闭集。 */
    public enum Shape { EMPTY, JSON_OBJECT, JSON_ARRAY, NUMERIC, BOOLEAN, LARGE_BLOB, PLAIN_TEXT }

    private final int blobThreshold;
    private final Map<Shape, AtomicLong> counters = new EnumMap<>(Shape.class);

    /** 默认阈值。 */
    public ToolArgShapeAudit() {
        this(DEFAULT_LARGE_BLOB_THRESHOLD);
    }

    /** 自定义大负载阈值（&lt;1 按默认）。 */
    public ToolArgShapeAudit(int blobThreshold) {
        this.blobThreshold = blobThreshold < 1 ? DEFAULT_LARGE_BLOB_THRESHOLD : blobThreshold;
        for (Shape shape : Shape.values()) {
            counters.put(shape, new AtomicLong());
        }
    }

    /** 分类单个参数并计数（null 按空串）。 */
    public Shape record(String arg) {
        Shape shape = classify(arg, blobThreshold);
        counters.get(shape).incrementAndGet();
        return shape;
    }

    /** 纯分类（不计数）：供只读探测复用。 */
    public static Shape classify(String arg, int blobThreshold) {
        String value = arg == null ? "" : arg.trim();
        if (value.isEmpty()) {
            return Shape.EMPTY;
        }
        if (value.length() > blobThreshold) {
            return Shape.LARGE_BLOB;
        }
        char first = value.charAt(0);
        if (first == '{' && value.endsWith("}")) {
            return Shape.JSON_OBJECT;
        }
        if (first == '[' && value.endsWith("]")) {
            return Shape.JSON_ARRAY;
        }
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Shape.BOOLEAN;
        }
        if (value.matches("-?\\d+(\\.\\d+)?([eE][-+]?\\d+)?")) {
            return Shape.NUMERIC;
        }
        return Shape.PLAIN_TEXT;
    }

    /** 形态→计数只读快照。 */
    public Map<Shape, Long> census() {
        Map<Shape, Long> out = new EnumMap<>(Shape.class);
        counters.forEach((shape, counter) -> out.put(shape, counter.get()));
        return Map.copyOf(out);
    }

    /** 已记录总数。 */
    public long total() {
        return counters.values().stream().mapToLong(AtomicLong::get).sum();
    }
}
