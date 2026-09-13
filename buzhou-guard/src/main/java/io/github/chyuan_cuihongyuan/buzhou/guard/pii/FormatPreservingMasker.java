package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * PII 格式保形掩码（spec 715 / T1030，Presidio format-preserving 思想的
 * 结构化简化版）：保留首尾可辨结构、只遮中段——下游按位数/分段解析的逻辑
 * 不被破坏（占位符 redact 是全替换，vault 是可逆代管——三形态各司其职）。
 *
 * <p>形状校验复用 {@link PiiDetector} 既有正则口径（检测与掩码两侧不漂移）；
 * 校验失败 → 等长全星（fail-closed 不抛——掩码管道里坏数据不炸流程）。
 * 非密码学 FPE：同输入同掩码——强去标识归 507 vault/SpillCipher 族。
 */
public final class FormatPreservingMasker {

    private static final Pattern PHONE_SHAPE = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern ID_CARD_SHAPE = Pattern.compile("\\d{17}[0-9Xx]");
    private static final Pattern EMAIL_SHAPE =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern IP_OCTET = Pattern.compile("\\d{1,3}");

    private FormatPreservingMasker() {
    }

    /** CN 手机：保前 3 后 4（138****5678）。 */
    public static String maskPhone(String phone) {
        Objects.requireNonNull(phone, "phone");
        if (!PHONE_SHAPE.matcher(phone).matches()) {
            return allStars(phone);
        }
        return mask(phone, 3, 4);
    }

    /** 18 位身份证：保前 4 后 2（尾位校验码可见）。 */
    public static String maskIdCard(String idCard) {
        Objects.requireNonNull(idCard, "idCard");
        if (!ID_CARD_SHAPE.matcher(idCard).matches()) {
            return allStars(idCard);
        }
        return mask(idCard, 4, 2);
    }

    /** 邮箱：保首字符与 @ 后域名（a***@example.com）。 */
    public static String maskEmail(String email) {
        Objects.requireNonNull(email, "email");
        if (!EMAIL_SHAPE.matcher(email).matches()) {
            return allStars(email);
        }
        int at = email.indexOf('@');
        return email.charAt(0) + "***" + email.substring(at);
    }

    /** IPv4：保前两段（192.168.*.*）。 */
    public static String maskIp(String ip) {
        Objects.requireNonNull(ip, "ip");
        String[] octets = ip.split("\\.", -1);
        if (octets.length != 4) {
            return allStars(ip);
        }
        for (String octet : octets) {
            if (!IP_OCTET.matcher(octet).matches()
                    || Integer.parseInt(octet) > 255) {
                return allStars(ip);
            }
        }
        return octets[0] + "." + octets[1] + ".*.*";
    }

    /** 通用保长中段打星（keepHead+keepTail ≥ 长度 → 全保不产生负中段）。 */
    public static String mask(String text, int keepHead, int keepTail) {
        Objects.requireNonNull(text, "text");
        if (keepHead < 0 || keepTail < 0) {
            throw new IllegalArgumentException("keepHead/keepTail 非负（当前 " + keepHead + "/" + keepTail + "）");
        }
        int length = text.length();
        if (keepHead + keepTail >= length) {
            return text;
        }
        return text.substring(0, keepHead) + "*".repeat(length - keepHead - keepTail)
                + text.substring(length - keepTail);
    }

    private static String allStars(String text) {
        return "*".repeat(text.length());
    }
}
