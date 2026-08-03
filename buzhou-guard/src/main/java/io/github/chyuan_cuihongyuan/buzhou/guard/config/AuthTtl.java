package io.github.chyuan_cuihongyuan.buzhou.guard.config;

/**
 * 授权时效（spec 07 授权语义）。
 *
 * <p>{@link #ONCE}（默认）：放行即消费，下次同指纹调用需重新授权。
 * <p>{@link #SESSION}：会话内长效，同类操作授权一次。
 */
public enum AuthTtl {
    ONCE,
    SESSION;

    public static AuthTtl parse(String value) {
        if (value == null || value.isBlank()) {
            return ONCE;
        }
        return "session".equalsIgnoreCase(value) ? SESSION : ONCE;
    }
}
