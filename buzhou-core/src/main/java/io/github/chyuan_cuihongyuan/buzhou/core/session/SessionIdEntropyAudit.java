package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.util.List;

/**
 * 会话 id 熵审计（spec 1404 / T2109 / impl 1057）——nanoid 熵计算器思想
 * （bits = length × log2(alphabet)，alphabet/length 决定抗碰撞强度）：
 * 会话 id 由宿主供给（{@code spawn} 入参），runtime 兜底 UUID——但弱 id
 * （时间戳、"user123" 式可猜串）无任何审计面。本审计对 id 做<b>下界估计</b>：
 * 从观测字符类保守推断生成字母表（含小写 ≥26、含数字再 +10……非字母数字
 * 按去重符号计），bits 下界不足即告警——不猜生成器，只报一致可算的保守值。
 *
 * <p>纯函数零状态零 IO；判定闭集 {@link Strength}（阈值常量显式）。
 * 与 SessionCapacityExceededException/迁移对账等会话治理面正交（只读不裁决）。
 */
public final class SessionIdEntropyAudit {

    /** 小写字母类规模（a-z）。 */
    private static final int LOWER_SIZE = 26;
    /** 大写字母类规模（A-Z）。 */
    private static final int UPPER_SIZE = 26;
    /** 数字类规模（0-9）。 */
    private static final int DIGIT_SIZE = 10;
    /** STRONG 下限（≥112 bits ≈ UUIDv4 的 122 bits 量级）。 */
    private static final double STRONG_FLOOR_BITS = 112;
    /** WEAK 上限（&lt;64 bits——典型时间戳/自增串落入此档）。 */
    private static final double WEAK_CEILING_BITS = 64;

    private SessionIdEntropyAudit() {
    }

    /** 强度闭集：INVALID（空/空白）/ WEAK / MODERATE / STRONG。 */
    public enum Strength { INVALID, WEAK, MODERATE, STRONG }

    /**
     * @param sessionId    被审 id 原文
     * @param length       长度（字符）
     * @param alphabetSize 观测推断的字母表下界（含类规模和 + 去重异类符号）
     * @param entropyBits  熵下界 = length × log2(alphabetSize)（INVALID 为 0）
     * @param strength     强度判定
     */
    public record Report(String sessionId, int length, int alphabetSize,
                         double entropyBits, Strength strength) {
    }

    /** 批量审计汇总：四桶计数（逐 id 判定见 {@link #audit(String)}）。 */
    public record Summary(long invalid, long weak, long moderate, long strong) {

        long total() {
            return invalid + weak + moderate + strong;
        }
    }

    /** 单 id 审计：空/空白 → INVALID；其余按字母表下界算熵分档。 */
    public static Report audit(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return new Report(sessionId, 0, 0, 0d, Strength.INVALID);
        }
        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        java.util.Set<Integer> otherSymbols = new java.util.TreeSet<>();
        for (int i = 0; i < sessionId.length(); i++) {
            char c = sessionId.charAt(i);
            if (c >= 'a' && c <= 'z') {
                hasLower = true;
            } else if (c >= 'A' && c <= 'Z') {
                hasUpper = true;
            } else if (c >= '0' && c <= '9') {
                hasDigit = true;
            } else {
                otherSymbols.add((int) c);
            }
        }
        int alphabet = (hasLower ? LOWER_SIZE : 0) + (hasUpper ? UPPER_SIZE : 0)
                + (hasDigit ? DIGIT_SIZE : 0) + otherSymbols.size();
        double bits = sessionId.length() * (Math.log(alphabet) / Math.log(2));
        Strength strength = bits < WEAK_CEILING_BITS ? Strength.WEAK
                : bits < STRONG_FLOOR_BITS ? Strength.MODERATE : Strength.STRONG;
        return new Report(sessionId, sessionId.length(), alphabet, bits, strength);
    }

    /** 批量审计：四桶计数（顺序无关）。 */
    public static Summary auditAll(List<String> sessionIds) {
        long invalid = 0;
        long weak = 0;
        long moderate = 0;
        long strong = 0;
        for (String id : sessionIds) {
            switch (audit(id).strength()) {
                case INVALID -> invalid++;
                case WEAK -> weak++;
                case MODERATE -> moderate++;
                case STRONG -> strong++;
            }
        }
        return new Summary(invalid, weak, moderate, strong);
    }
}
