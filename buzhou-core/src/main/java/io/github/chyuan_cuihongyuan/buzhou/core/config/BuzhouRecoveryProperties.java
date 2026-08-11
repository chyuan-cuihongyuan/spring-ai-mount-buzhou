package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.recovery.DurabilityTier;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RecoveryConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ResumeStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 崩溃中轮次恢复 + 幂等装配属性（spec「崩溃中轮次恢复 M1」，前缀 {@code buzhou.recovery}）。
 *
 * <p>字段统一用 boxed 类型，null = 「未配置」→ 取规范默认（对齐 {@code SpillProperties} /
 * {@code ResilienceProperties} 模板），yml 只覆盖个别参数时其余仍取默认。
 * M1 地板：默认 + yml 两层（绑定级覆盖待 policy 消费管线打通再纳入，与韧性层 03 同口径）。
 *
 * @param enabled            机制总开关（默认开，safe-by-default；关则回退底座原生行为——
 *                           租约不续约、无恢复事件、不去重、写路径不分档）
 * @param leaseTtl           会话租约 TTL（默认 90s，spec 08）；轮次执行期由心跳续约
 * @param heartbeatInterval  租约心跳续约间隔（默认 30s，约为 TTL 的 1/3）
 * @param durabilityTier     持久化强度档位（{@code SYNC} / {@code ASYNC} / {@code EXIT}；
 *                           默认 {@code ASYNC}，大小写无关）
 * @param resumeStrategy     恢复语义档位（{@code VOID} / {@code AUTO_RESUME}；默认 {@code VOID}
 *                           ——不擅自续跑；{@code AUTO_RESUME} 为无人值守/长任务会话 opt-in）
 * @param crashloopHardCap   自动重驱动崩溃循环硬顶次数（默认 3；03/04 熔断就绪前的保守闸门）
 * @param idempotencyEnabled 幂等去重开关（默认开：副作用工具崩溃 + 恢复后效果恰好一次；
 *                           一键关闭排障时回退基线行为）
 */
@ConfigurationProperties(prefix = "buzhou.recovery")
public record BuzhouRecoveryProperties(
        Boolean enabled,
        Duration leaseTtl,
        Duration heartbeatInterval,
        String durabilityTier,
        String resumeStrategy,
        Integer crashloopHardCap,
        Boolean idempotencyEnabled) {

    public BuzhouRecoveryProperties {
        enabled = enabled == null || enabled;
        idempotencyEnabled = idempotencyEnabled == null || idempotencyEnabled;
    }

    /** 全默认（装配测试 / 兜底用）。 */
    public static BuzhouRecoveryProperties defaults() {
        return new BuzhouRecoveryProperties(null, null, null, null, null, null, null);
    }

    /** 翻译为运行时配置（枚举大小写无关，非法值落回规范默认）。 */
    public RecoveryConfig toRecoveryConfig() {
        return new RecoveryConfig(
                enabled,
                leaseTtl,
                heartbeatInterval,
                parseEnum(DurabilityTier.class, durabilityTier, DurabilityTier.ASYNC),
                parseEnum(ResumeStrategy.class, resumeStrategy, ResumeStrategy.VOID),
                crashloopHardCap == null ? RecoveryConfig.DEFAULT_CRASHLOOP_HARD_CAP : crashloopHardCap,
                idempotencyEnabled);
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, E fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
