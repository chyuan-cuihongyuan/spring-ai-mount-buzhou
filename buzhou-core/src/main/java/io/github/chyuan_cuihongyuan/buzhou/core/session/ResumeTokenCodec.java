package io.github.chyuan_cuihongyuan.buzhou.core.session;

/**
 * 续读令牌编解码与裁决（spec 1833 / T2867 / impl 1434）——分页
 * continuation token / 条件请求 ETag 思想：令牌绑定签发时的数据指纹——
 * 续读不只是「从哪个偏移继续」，更是「底层数据还是不是那一版」：指纹
 * 不符即令牌过期（STALE_DATA，游标语义已失效——底层数据换代了），越界
 * （OUT_OF_RANGE）与可续（VALID）分开裁决。
 *
 * <p>纯函数零状态、只裁决不读取（数据访问归宿主）。
 */
public final class ResumeTokenCodec {

    /** 编码分隔符（指纹与偏移之间）。 */
    public static final String SEPARATOR = "@";

    private ResumeTokenCodec() {
    }

    /** 续读令牌契约：指纹非空白、偏移 ≥ 0。 */
    public record ResumeToken(String fingerprint, long offset) {

        public ResumeToken {
            if (fingerprint == null || fingerprint.isBlank() || offset < 0) {
                throw new IllegalArgumentException(String.format(
                        "非法令牌：fingerprint=%s, offset=%d（要求指纹非空白且偏移 ≥ 0）",
                        fingerprint, offset));
            }
        }
    }

    /** 续读裁决三态：VALID 可续 / STALE_DATA 数据换代 / OUT_OF_RANGE 越界。 */
    public enum Verdict {

        /** 指纹相符且未越界——可续读。 */
        VALID,

        /** 指纹不符——底层数据已换代，游标语义失效。 */
        STALE_DATA,

        /** 偏移超出当前水位——越界。 */
        OUT_OF_RANGE
    }

    /** 编码：fingerprint + {@link #SEPARATOR} + offset。 */
    public static String encode(ResumeToken token) {
        return token.fingerprint() + SEPARATOR + token.offset();
    }

    /** 解码（畸形令牌 fail-fast：无分隔符/负偏移/空指纹）。 */
    public static ResumeToken decode(String token) {
        if (token == null) {
            throw new IllegalArgumentException("令牌不能为 null");
        }
        int at = token.lastIndexOf(SEPARATOR);
        if (at <= 0 || at == token.length() - 1) {
            throw new IllegalArgumentException("畸形令牌（缺指纹或偏移）：" + token);
        }
        long offset;
        try {
            offset = Long.parseLong(token.substring(at + 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("畸形令牌（偏移非数字）：" + token, e);
        }
        return new ResumeToken(token.substring(0, at), offset);
    }

    /**
     * 续读裁决。契约：currentFingerprint 非空白、currentMax ≥ 0（fail-fast）；
     * 语义：指纹先比（不符即 STALE_DATA——换代优先于越界），再比偏移
     *（offset &gt; currentMax 即 OUT_OF_RANGE；== max 即「读到尾」VALID）。
     */
    public static Verdict check(ResumeToken token, String currentFingerprint, long currentMax) {
        if (currentFingerprint == null || currentFingerprint.isBlank()) {
            throw new IllegalArgumentException("currentFingerprint 不能为空");
        }
        if (currentMax < 0) {
            throw new IllegalArgumentException("currentMax 不能为负：" + currentMax);
        }
        if (!token.fingerprint().equals(currentFingerprint)) {
            return Verdict.STALE_DATA;
        }
        return token.offset() > currentMax ? Verdict.OUT_OF_RANGE : Verdict.VALID;
    }
}
