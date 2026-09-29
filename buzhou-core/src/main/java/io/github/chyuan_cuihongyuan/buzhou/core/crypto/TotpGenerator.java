package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * TOTP 动态口令（spec 8038 / V8077 / impl 2340）——
 * RFC 6238/4226 Google Authenticator 思想：**HOTP(计数)=
 * HMAC-SHA1(secret, 计数 8 字节大端) 动态截断 mod 10^digits；
 * TOTP=HOTP(时间/步长)**——步长/计数注入（零真实时钟依赖）
 * 完全确定；窗口 verify 重放语义；RFC 4226 十向量 + RFC 6238
 * T=59 → 94287082 官方向量钉死；null fail-fast。
 *
 * <p>与 ConstantTimeEquals（spec 8037）同族不同面：生成/
 * 校验面 vs 比较面。
 */
public final class TotpGenerator {

    private static final int TIME_STEP_SECONDS = 30;

    private TotpGenerator() {
    }

    /** HOTP：RFC 4226（8 字节计数大端 + HMAC-SHA1 动态截断）。 */
    public static int hotp(byte[] secret, long counter, int digits) {
        byte[] counterBytes = new byte[8];
        long value = counter;
        for (int i = 7; i >= 0; i--) {
            counterBytes[i] = (byte) value;
            value >>>= 8;
        }
        byte[] mac = hmacSha1(secret, counterBytes);
        int offset = mac[mac.length - 1] & 0x0F;
        int binary = ((mac[offset] & 0x7F) << 24)
                | ((mac[offset + 1] & 0xFF) << 16)
                | ((mac[offset + 2] & 0xFF) << 8)
                | (mac[offset + 3] & 0xFF);
        int modulus = 1;
        for (int i = 0; i < digits; i++) {
            modulus *= 10;
        }
        return binary % modulus;
    }

    /** TOTP：HOTP(时间步计数)（unixSecond/步长——时钟注入确定）。 */
    public static int totp(byte[] secret, long unixSecond, int digits) {
        return hotp(secret, unixSecond / TIME_STEP_SECONDS, digits);
    }

    /** 窗口校验（±window 步内任一匹配即真——重放语义明示）。 */
    public static boolean verify(byte[] secret, long unixSecond, int code, int digits, int window) {
        long counter = unixSecond / TIME_STEP_SECONDS;
        for (long offset = -window; offset <= window; offset++) {
            if (hotp(secret, counter + offset, digits) == code) {
                return true;
            }
        }
        return false;
    }

    /** RFC 6238 默认 6 位便捷面。 */
    public static int totp6(byte[] secret, long unixSecond) {
        return totp(secret, unixSecond, 6);
    }

    private static byte[] hmacSha1(byte[] secret, byte[] message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret, "HmacSHA1"));
            return mac.doFinal(message);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA1 不可用（JDK 环境异常）", e);
        }
    }
}
