package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规则式 PII 检测器（spec 86 §A / T329，Microsoft Presidio 借鉴——无 ML 依赖的
 * 子集）：邮箱 / 手机号 / 身份证号（GB 11643 校验位验证）/ 银行卡号（Luhn 验证）/
 * IPv4（0-255 段验证）。校验位/段验证收窄误报——长数字串不因「看起来像」被误杀。
 *
 * <p>诚实边界：规则式召回有限（无姓名/地址等 NER 面）——Presidio 的
 * RecognizerPlugin SPI 扩展点在 fog 台账，不预设。
 */
public final class PiiDetector {

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

    /** 命中指定类型集即脱敏（占位符形态 {@code [PII:TYPE]}）。 */
    public String redact(String text, Set<PiiType> enabled) {
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
