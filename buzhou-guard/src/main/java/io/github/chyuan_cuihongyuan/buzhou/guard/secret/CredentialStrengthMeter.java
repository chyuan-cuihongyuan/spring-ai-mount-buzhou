package io.github.chyuan_cuihongyuan.buzhou.guard.secret;

/**
 * 凭据强度计（spec 1863 / T2927 / impl 1464）——密码强度计惯例（zxcvbn
 * 简化口径/NIST SP 800-63 长度优先思想）：**评估已知凭据**的弱度——字符
 * 类别多样性（小写/大写/数字/符号四类）× 长度阶梯双条件定档；与
 * {@link SecretScanner}（检测**文本中**的秘密，truffleHog 熵口径）互补：
 * 扫描器防泄漏、强度计防弱钥。弱钥告警（测试配置里的弱 API key、生成
 * 的默认口令）在落盘前显形。
 *
 * <p>纯函数零状态；类别多样性是必要非充分条件（真实强度还看不可预测
 * 性——诚实边界入档，深度评分归 zxcvbn 类未来静脉）。
 */
public final class CredentialStrengthMeter {

    /** FAIR 档最小长度。 */
    public static final int FAIR_MIN_LENGTH = 12;

    /** STRONG 档最小长度（长度优先——每加一字符熵指数涨）。 */
    public static final int STRONG_MIN_LENGTH = 16;

    /** 进 FAIR/STRONG 需要的最少字符类别数。 */
    public static final int MIN_CLASSES = 3;

    /** 强度三档：WEAK 弱 / FAIR 及格 / STRONG 强。 */
    public enum Band {

        /** 弱——长度或多样性不达标，告警换钥。 */
        WEAK,

        /** 及格——可用但建议加强。 */
        FAIR,

        /** 强——长度与多样性双达标。 */
        STRONG
    }

    /** 字符类别数：小写/大写/数字/符号各计 1（0–4）。契约：凭据非空白。 */
    public static int charClasses(String credential) {
        requireCredential(credential);
        boolean lower = false;
        boolean upper = false;
        boolean digit = false;
        boolean symbol = false;
        for (int i = 0; i < credential.length(); i++) {
            char c = credential.charAt(i);
            if (Character.isLowerCase(c)) {
                lower = true;
            } else if (Character.isUpperCase(c)) {
                upper = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            } else {
                symbol = true;
            }
        }
        int classes = 0;
        classes += lower ? 1 : 0;
        classes += upper ? 1 : 0;
        classes += digit ? 1 : 0;
        classes += symbol ? 1 : 0;
        return classes;
    }

    /**
     * 强度定档（双条件阶梯）：类别 ≥ {@value #MIN_CLASSES} 且长度 ≥
     * {@value #STRONG_MIN_LENGTH} → STRONG；类别 ≥ {@value #MIN_CLASSES}
     * 且长度 ≥ {@value #FAIR_MIN_LENGTH} → FAIR；否则 WEAK。
     */
    public static Band band(String credential) {
        requireCredential(credential);
        int classes = charClasses(credential);
        if (classes < MIN_CLASSES) {
            return Band.WEAK;
        }
        if (credential.length() >= STRONG_MIN_LENGTH) {
            return Band.STRONG;
        }
        if (credential.length() >= FAIR_MIN_LENGTH) {
            return Band.FAIR;
        }
        return Band.WEAK;
    }

    private static void requireCredential(String credential) {
        if (credential == null || credential.isBlank()) {
            throw new IllegalArgumentException("凭据不能为空白");
        }
    }
}
