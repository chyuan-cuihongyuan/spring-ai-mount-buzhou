package io.github.chyuan_cuihongyuan.buzhou.guard.config;

import java.util.List;

/**
 * HITL 危险守卫配置（spec 07 配置项，前缀 {@code buzhou.guard.*}）。
 *
 * @param enabled        守卫总开关
 * @param authTtl        授权时效：{@link AuthTtl#ONCE}（默认，一次性消费）/ {@link AuthTtl#SESSION}（会话内长效）
 * @param dangerousTools 危险工具清单；默认空 = 无拦截
 */
public record DangerousToolConfig(boolean enabled, AuthTtl authTtl, List<DangerousToolEntry> dangerousTools) {

    public DangerousToolConfig {
        authTtl = authTtl == null ? AuthTtl.ONCE : authTtl;
        dangerousTools = dangerousTools == null ? List.of() : List.copyOf(dangerousTools);
    }

    public static DangerousToolConfig defaults() {
        return new DangerousToolConfig(true, AuthTtl.ONCE, List.of());
    }
}
