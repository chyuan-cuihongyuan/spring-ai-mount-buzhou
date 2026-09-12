package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规则式 PII 检测器（spec 86 §A / T329，Microsoft Presidio 借鉴——无 ML 依赖的
 * 子集）：邮箱 / 手机号 / 身份证号（GB 11643 校验位验证）/ 银行卡号（Luhn 验证）/
 * IPv4（0-255 段验证）。校验位/段验证收窄误报——长数字串不因「看起来像」被误杀。
 *
 * <p>诚实边界：规则式召回有限（无姓名/地址等 NER 面）——Presidio 的
 * RecognizerPlugin SPI 扩展点在 fog 台账，不预设。spec 713：pseudonymize
 * 格式保持假名化（同长度同形态替身——保形状不保校验位，诚实划界见方法注）。
 */
public final class PiiDetector {

    /** 假名化播种基（确定性参数非安全参数——遮蔽非加密，spec 713）。 */
    static final long SURROGATE_SEED = 0x62757A686F75L;
    /** splitmix64 黄金常数（0x9E3779B97F4A7C15）。 */
    private static final long SPLITMIX_GAMMA = 0x9E3779B97F4A7C15L;

    private final boolean formatPreserving;

    /** 默认构造：MASK 模式（redact = 全占位符——既有语义）。 */
    public PiiDetector() {
        this(false);
    }

    /**
     * spec 731 / T1013：模式构造——{@code formatPreserving=true} 时
     * {@link #redact} 分派到 {@link #pseudonymize}（同长度同形态替身）；
     * false = 既有 MASK 语义。三缝（输入/出站/流式）共用同一 detector 实例
     * 时模式自然一致。
     */
    public PiiDetector(boolean formatPreserving) {
        this.formatPreserving = formatPreserving;
    }

    /** 单次命中（type + 区间；text 仅测试/审计用，勿外发日志）。 */
    public record PiiMatch(PiiType type, int start, int end, String text) {
    }

    private static final Pattern EMAIL =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern CN_PHONE =
            Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private static final Pattern CN_RESIDENT_ID =
            Pattern.compile("(?<!\\d)\\d{17}[0-9Xx](?!\\d)");
    private static final Pattern BANK_CARD =
            Pattern.compile("(?<!\\d)\\d{13,19}(?!\\d)");
    private static final Pattern IPV4 =
            Pattern.compile("(?<![\\d.])((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}"
                    + "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(?![\\d.])");
    /** GB 11643-1999 校验位权重与映射。 */
    private static final int[] ID_WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static final String ID_CHECK_CODES = "10X98765432";

    /** 扫描（区间升序；重叠去重取先出现者——数字型优先级由扫描顺序决定）。 */
    /** 扫描（区间升序；重叠去重取先出现者——数字型优先级由扫描顺序决定）。 */
    public List<PiiMatch> scan(String text) {
        List<PiiMatch> matches = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return matches;
        }
        collect(matches, text, EMAIL, PiiType.EMAIL, null);
        collect(matches, text, CN_RESIDENT_ID, PiiType.CN_RESIDENT_ID, PiiDetector::validResidentId);
        collect(matches, text, BANK_CARD, PiiType.BANK_CARD, PiiDetector::validLuhn);
        collect(matches, text, CN_PHONE, PiiType.CN_PHONE, null);
        collect(matches, text, IPV4, PiiType.IPV4, null);
        matches.sort(Comparator.comparingInt(PiiMatch::start));
        return dedupeOverlaps(matches);
    }

    /**
     * 格式保持假名化（spec 713 / T977，Presidio surrogate 思想）：命中段生成
     * <b>同长度同字符形态</b>替身——数字→伪随机数字、字母→同大小写字母、
     * 分隔符/空白/符号原样。<b>确定性</b>：随机流按 (SURROGATE_SEED, type,
     * 命中文本) 哈希播种——同 (type,文本) 恒同替身（进程内外一致），无共享
     * 可变态（线程安全）。<b>不可逆</b>（原文零留痕——可逆需求归 PiiVault）。
     *
     * <p><b>诚实边界</b>：保形状不保校验位——身份证 mod-11 / 银行卡 Luhn 在
     * 替身上不再验真（校验失败是遮蔽生效的预期信号）；真 FPE/FF1 需密码学
     * 实现，非目标。SURROGATE_SEED 是确定性参数非安全参数（遮蔽非加密）。
     */
    public String pseudonymize(String text, Set<PiiType> enabled) {
        if (text == null || text.isEmpty() || enabled.isEmpty()) {
            return text;
        }
        List<PiiMatch> hits = scan(text).stream()
                .filter(m -> enabled.contains(m.type())).toList();
        if (hits.isEmpty()) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        int cursor = 0;
        for (PiiMatch hit : hits) {
            out.append(text, cursor, hit.start());
            out.append(surrogate(hit.type(), hit.text()));
            cursor = hit.end();
        }
        out.append(text.substring(cursor));
        return out.toString();
    }

    /** 替身生成：逐字符保形态（数字/字母/其它），伪随机流 = splitmix64 整数混排（无共享态）。 */
    private static String surrogate(PiiType type, String hitText) {
        long state = mix((long) Objects.hash(SURROGATE_SEED, type.name(), hitText));
        StringBuilder out = new StringBuilder(hitText.length());
        for (int i = 0; i < hitText.length(); i++) {
            state += SPLITMIX_GAMMA;
            state = mix(state);
            char c = hitText.charAt(i);
            if (Character.isDigit(c)) {
                out.append((char) ('0' + (int) Long.remainderUnsigned(state, 10)));
            } else if (c >= 'a' && c <= 'z') {
                out.append((char) ('a' + (int) Long.remainderUnsigned(state >>> 1, 26)));
            } else if (c >= 'A' && c <= 'Z') {
                out.append((char) ('A' + (int) Long.remainderUnsigned(state >>> 1, 26)));
            } else {
                out.append(c); // 分隔符/空白/符号原样——形状保持
            }
        }
        return out.toString();
    }

    /** splitmix64 终态混排（确定性整数扩散——非加密原语）。 */
    private static long mix(long x) {
        x += SPLITMIX_GAMMA;
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return x ^ (x >>> 31);
    }

    /** 命中指定类型集即脱敏（MODE 分派——spec 731：formatPreserving → pseudonymize）。 */
    public String redact(String text, Set<PiiType> enabled) {
        return formatPreserving ? pseudonymize(text, enabled) : maskRedact(text, enabled);
    }

    /** MASK 模式：占位符形态 {@code [PII:TYPE]}（既有语义）。 */
    private String maskRedact(String text, Set<PiiType> enabled) {
        if (text == null || text.isEmpty() || enabled.isEmpty()) {
            return text;
        }
        List<PiiMatch> hits = scan(text).stream()
                .filter(m -> enabled.contains(m.type())).toList();
        if (hits.isEmpty()) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        int cursor = 0;
        for (PiiMatch hit : hits) {
            out.append(text, cursor, hit.start());
            out.append("[PII:").append(hit.type().name()).append(']');
            cursor = hit.end();
        }
        out.append(text.substring(cursor));
        return out.toString();
    }

    private static void collect(List<PiiMatch> out, String text, Pattern pattern,
            PiiType type, java.util.function.Predicate<String> validator) {
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            if (validator == null || validator.test(m.group())) {
                out.add(new PiiMatch(type, m.start(), m.end(), m.group()));
            }
        }
    }

    /** 去重叠（同区间多类型保先注册者；区间包含/相交保先出现者）。 */
    private static List<PiiMatch> dedupeOverlaps(List<PiiMatch> sorted) {
        List<PiiMatch> out = new ArrayList<>();
        int lastEnd = -1;
        for (PiiMatch m : sorted) {
            if (m.start() >= lastEnd) {
                out.add(m);
                lastEnd = m.end();
            }
        }
        return out;
    }

    /** GB 11643-1999 mod-11-2 校验位。 */
    static boolean validResidentId(String id) {
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (id.charAt(i) - '0') * ID_WEIGHTS[i];
        }
        char expected = ID_CHECK_CODES.charAt(sum % 11);
        return Character.toUpperCase(id.charAt(17)) == expected;
    }

    /** Luhn 算法（银行卡号校验）。 */
    static boolean validLuhn(String digits) {
        int sum = 0;
        boolean dbl = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (dbl) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            dbl = !dbl;
        }
        return sum % 10 == 0;
    }

    static Set<PiiType> allTypes() {
        return EnumSet.allOf(PiiType.class);
    }
}
