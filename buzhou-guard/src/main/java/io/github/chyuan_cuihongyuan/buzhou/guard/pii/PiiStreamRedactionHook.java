package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.StreamTextFilter;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 模型回复出站 PII 脱敏 hook（spec 500 / T752，Presidio 流式匿名化 + 流式 WAF
 * 回看窗口思想）：输入缝（106）与工具输出缝（86）之外的第三缝——模型自产/复读
 * 的 PII 离场前占位符化。流式 chunk 级滑动窗口缓冲：可定界前缀只发安全前缀
 * （emitLen = len−window+1——未来 match 覆盖不到已发字符），跨界实体留窗待完整
 * 再脱敏；flush 全量排空。order 75（PII 族同列：输入 60 / 工具输出 70 / 回复 75）。
 *
 * <p>命中计数复用 {@code buzhou.guard.pii.redactions}（type tag）+ PiiHitStats
 * Side.OUTPUT（回复即出站面，不新开 side 枚举）。
 *
 * <p>诚实边界：窗口（默认 128 = RFC 5321 邮箱本地段 64 + 常见域名裕量；固定型
 * 上限 19 全覆盖）放不下的超长跨界实体不保；会话历史内的模型自产 PII 不回溯
 * 清洗（出站缝每轮拦截，用户面恒净）。
 */
public class PiiStreamRedactionHook implements BuzhouHook {

    public static final int ORDER = 75;
    /** 默认回看窗口（字符）。 */
    public static final int DEFAULT_WINDOW = 128;

    private final PiiDetector detector;
    private final Set<PiiType> enabledTypes;
    private final CustomPiiRules customRules;
    private final int window;

    public PiiStreamRedactionHook() {
        this(EnumSet.allOf(PiiType.class));
    }

    public PiiStreamRedactionHook(Set<PiiType> enabledTypes) {
        this(enabledTypes, null, DEFAULT_WINDOW);
    }

    /** 全参构造：types null = 全类型；customRules null = 无自定义；window 必须 ≥ 1。 */
    public PiiStreamRedactionHook(Set<PiiType> enabledTypes, CustomPiiRules customRules, int window) {
        if (window < 1) {
            throw new IllegalArgumentException("reply window 必须 ≥ 1（实际 " + window + "）");
        }
        this.detector = new PiiDetector();
        this.enabledTypes = EnumSet.copyOf(enabledTypes == null || enabledTypes.isEmpty()
                ? EnumSet.allOf(PiiType.class) : enabledTypes);
        this.customRules = customRules == null ? new CustomPiiRules(List.of()) : customRules;
        this.window = window;
    }

    @Override
    public String name() {
        return "PiiStreamRedactionHook";
    }

    @Override
    public int order() {
        return ORDER;
    }

    /** 每轮新建窗口过滤器（有状态——跨 chunk 缓冲；同轮串行回调契约，无需线程安全）。 */
    @Override
    public StreamTextFilter replyStreamFilter() {
        return new WindowFilter();
    }

    private final class WindowFilter implements StreamTextFilter {

        private final StringBuilder pending = new StringBuilder();

        @Override
        public String filter(String chunk) {
            if (chunk == null || chunk.isEmpty()) {
                return "";
            }
            pending.append(chunk);
            return drain(false);
        }

        @Override
        public String flush() {
            return drain(true);
        }

        /** 脱敏在窗完整实体，发出安全前缀（flush 时全量）；占位符不拆分跨界发出。 */
        private String drain(boolean finalPass) {
            if (pending.length() == 0) {
                return "";
            }
            redactPending();
            int emitLength = finalPass
                    ? pending.length()
                    : Math.max(0, pending.length() - window + 1);
            emitLength = clampToPlaceholderBoundary(emitLength);
            if (emitLength == 0) {
                return "";
            }
            String outbound = pending.substring(0, emitLength);
            pending.delete(0, emitLength);
            return outbound;
        }

        /** emit 边界落在占位符内部时回退到占位符起点——下游所见占位符恒完整。 */
        private int clampToPlaceholderBoundary(int emitLength) {
            if (emitLength <= 0 || emitLength >= pending.length()) {
                return emitLength;
            }
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("\\[PII:[A-Z0-9_]{2,32}\\]").matcher(pending);
            while (matcher.find()) {
                if (matcher.start() < emitLength && emitLength < matcher.end()) {
                    return matcher.start();
                }
            }
            return emitLength;
        }

        /** 全窗扫描脱敏（内置类型 + 自定义规则叠加）。占位符对检测正则惰性——
         * 窗中旧占位符不影响新实体检出，redacted==text 引用等零命中零计数（天然幂等）。 */
        private void redactPending() {
            String text = pending.toString();
            List<PiiDetector.PiiMatch> hits = detector.scan(text).stream()
                    .filter(m -> enabledTypes.contains(m.type())).toList();
            String redacted = detector.redact(text, enabledTypes);
            if (!customRules.isEmpty()) {
                redacted = customRules.redact(redacted);
            }
            if (redacted == text) {
                return; // 引用等——零命中
            }
            for (PiiDetector.PiiMatch match : hits) {
                BuzhouMetricsHolder.metrics().counter("buzhou.guard.pii.redactions",
                        "type", match.type().name());
                PiiHitStats.global().record(match.type(), PiiHitStats.Side.OUTPUT);
            }
            if (!customRules.isEmpty()) {
                for (String ruleName : customRuleHits(redacted)) {
                    PiiHitStats.global().recordCustom(ruleName, PiiHitStats.Side.OUTPUT);
                }
            }
            pending.replace(0, pending.length(), redacted);
        }

        /** 从脱敏产物提取自定义规则命中名（内置类型名剔除——PiiRedactionHook 同法）。 */
        private List<String> customRuleHits(String redacted) {
            List<String> names = new java.util.ArrayList<>();
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("\\[PII:([A-Z0-9_]{2,32})\\]").matcher(redacted);
            while (matcher.find()) {
                String name = matcher.group(1);
                boolean builtIn = false;
                for (PiiType type : PiiType.values()) {
                    if (type.name().equals(name)) {
                        builtIn = true;
                        break;
                    }
                }
                if (!builtIn) {
                    names.add(name);
                }
            }
            return names;
        }
    }
}
