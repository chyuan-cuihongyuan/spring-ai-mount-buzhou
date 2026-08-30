package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 错误签名聚类注册表（spec 83 §A / T321，Sentry fingerprint 借鉴）：失败按
 * 「异常简名 + 归一化首行」聚成有界签名——看板看 top 错误族而非逐条日志。
 *
 * <p><b>为什么不进 micrometer tag</b>：签名维度天生非有界枚举（项目纪律「tag 值
 * 有界」），故本表是进程内有界 Map（{@value #MAX_SIGNATURES} 条封顶，超出折入
 * {@code __overflow__}），经 {@link #top(int)} 供健康端点/看板拉取；与
 * {@link BuzhouMetricsHolder} 同款全局旋钮模式（Netty ResourceLeakDetector 先例）。
 *
 * <p><b>归一化</b>：取消息首行；数字串折 {@code #}（时间戳/大小/端口不再分裂族）；
 * 十六进制串 ≥8 位折 {@code 0x#}；空白压缩；截 {@value #MAX_SIGNATURE_LENGTH} 字符。
 */
public final class ErrorSignatures {

    public static final int MAX_SIGNATURES = 256;
    public static final int MAX_SIGNATURE_LENGTH = 96;
    static final String OVERFLOW = "__overflow__";

    private static final AtomicReference<ErrorSignatures> GLOBAL =
            new AtomicReference<>(new ErrorSignatures());

    private final Map<String, AtomicLong> counts = new ConcurrentHashMap<>();

    private ErrorSignatures() {
    }

    /** 独立实例（测试/宿主自管作用域用；默认走 {@link #global()}）。 */
    public static ErrorSignatures create() {
        return new ErrorSignatures();
    }

    public static ErrorSignatures global() {
        return GLOBAL.get();
    }

    /** 测试替换/清理（null = 换新；@AfterEach 纪律）。 */
    public static void install(ErrorSignatures registry) {
        GLOBAL.set(registry == null ? new ErrorSignatures() : registry);
    }

    /** 记一次失败（kind 进签名前缀：tool/model/…——有界由调用方纪律保证）。 */
    public void record(String kind, Throwable error) {
        record(kind, error == null ? "unknown" : error.getClass().getSimpleName()
                + ":" + String.valueOf(error.getMessage()));
    }

    /** 记一次失败（文本面——模型侧非异常错误码等）。 */
    public void record(String kind, String errorText) {
        String sig = signature(kind, errorText);
        AtomicLong counter = counts.get(sig);
        if (counter != null) {
            counter.incrementAndGet();
            return;
        }
        // 封顶语义：满且新签名 → 折 <kind>:__overflow__（既有族继续细分，新族不再
        // 扩张；kind 各自的 overflow 分开计数——看板按错误面分组不被合并）。并发双插
        // 最坏轻微超限 1-2 条，无正确性影响
        if (counts.size() >= MAX_SIGNATURES) {
            counts.computeIfAbsent(kind + ":" + OVERFLOW, k -> new AtomicLong()).incrementAndGet();
            return;
        }
        counts.computeIfAbsent(sig, k -> new AtomicLong()).incrementAndGet();
    }

    /** top-N（count 降序，同 count 签名字典序——输出稳定）。 */
    public List<Map.Entry<String, Long>> top(int n) {
        return counts.entrySet().stream()
                .sorted((a, b) -> {
                    int byCount = Long.compare(b.getValue().get(), a.getValue().get());
                    return byCount != 0 ? byCount : a.getKey().compareTo(b.getKey());
                })
                .limit(Math.max(0, n))
                .map(e -> Map.entry(e.getKey(), e.getValue().get()))
                .toList();
    }

    /** 按 kind 前缀过滤的 top-N（spec 196 §A / T558：kind + ":" 是签名前缀——
     * 「只看模型侧错误族」这类分面看板；空白 kind = IllegalArgumentException）。 */
    public List<Map.Entry<String, Long>> top(String kind, int n) {
        if (kind == null || kind.isBlank()) {
            throw new IllegalArgumentException("kind must not be blank");
        }
        String prefix = kind + ":";
        return counts.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .sorted((a, b) -> {
                    int byCount = Long.compare(b.getValue().get(), a.getValue().get());
                    return byCount != 0 ? byCount : a.getKey().compareTo(b.getKey());
                })
                .limit(Math.max(0, n))
                .map(e -> Map.entry(e.getKey(), e.getValue().get()))
                .toList();
    }

    /** 签名计数快照（只读；健康端点用）。 */
    public Map<String, Long> snapshot() {
        Map<String, Long> out = new java.util.LinkedHashMap<>();
        counts.entrySet().stream()
                .sorted((a, b) -> {
                    int byCount = Long.compare(b.getValue().get(), a.getValue().get());
                    return byCount != 0 ? byCount : a.getKey().compareTo(b.getKey());
                })
                .forEach(e -> out.put(e.getKey(), e.getValue().get()));
        return java.util.Collections.unmodifiableMap(out);
    }

    /** 在册签名数（含 overflow 占位——测试/健康面）。 */
    public int distinct() {
        return counts.size();
    }

    /**
     * 清零（spec 121 §A / T423，spec 112 fog「导出后清零」）：窗口化统计——
     * export → reset 循环即「每窗口一份 JSONL、进程内表永有界」。
     */
    public void reset() {
        counts.clear();
    }

    static String signature(String kind, String errorText) {
        String firstLine = errorText == null ? "" : errorText.lines().findFirst().orElse("");
        // 单趟归一（替换符不含数字——避免被后续趟误折）：长十六进制 → hex#，数字串 → #
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("[0-9a-fA-F]{8,}|[0-9]+").matcher(firstLine);
        StringBuilder normalized = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(normalized, m.group().length() >= 8 ? "hex#" : "#");
        }
        m.appendTail(normalized);
        String out = normalized.toString().replaceAll("\\s+", " ").trim();
        if (out.length() > MAX_SIGNATURE_LENGTH) {
            out = out.substring(0, MAX_SIGNATURE_LENGTH);
        }
        return kind + ":" + (out.isEmpty() ? "<blank>" : out);
    }
}
