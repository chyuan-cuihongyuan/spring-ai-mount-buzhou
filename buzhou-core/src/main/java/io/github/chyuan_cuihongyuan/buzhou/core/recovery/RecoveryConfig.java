package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import java.time.Duration;

/**
 * 崩溃恢复 + 幂等机制的运行时配置（spec「崩溃中轮次恢复 M1」）。
 *
 * <p>本 record 是机制内部的运行时值对象：Spring 层的 {@code @ConfigurationProperties} record
 * （{@code BuzhouRecoveryProperties} / {@code BuzhouCoreProperties.Session.Lease}）在自装配层
 * 翻译为本对象，经 {@code DefaultAgentRuntime} / {@code HarnessAssembler} 消费。M1 地板：
 * 默认 + yml 两层（绑定级覆盖待 policy 消费管线打通再纳入，与韧性层 03 同口径）。
 *
 * <p>safe-by-default：默认 {@link DurabilityTier#ASYNC} 档、{@link ResumeStrategy#VOID} 恢复、
 * 幂等去重开、自动重驱动关；{@link #enabled} 总开关关则整体回退底座原生行为。
 *
 * @param enabled             机制总开关（默认开）
 * @param leaseTtl            会话租约 TTL（默认 90s，spec 08）；轮次执行期由心跳续约
 * @param heartbeatInterval   租约心跳续约间隔（默认 30s，约为 TTL 的 1/3）
 * @param durabilityTier      持久化强度档位（默认 {@link DurabilityTier#ASYNC}）
 * @param resumeStrategy      恢复语义档位（默认 {@link ResumeStrategy#VOID}）
 * @param crashloopHardCap    自动重驱动崩溃循环硬顶次数（M1 兜底；03/04 熔断就绪前的保守闸门）
 * @param idempotencyEnabled  幂等去重开关（默认开：副作用工具崩溃 + 恢复后效果恰好一次）
 */
public record RecoveryConfig(
        boolean enabled,
        Duration leaseTtl,
        Duration heartbeatInterval,
        DurabilityTier durabilityTier,
        ResumeStrategy resumeStrategy,
        int crashloopHardCap,
        boolean idempotencyEnabled) {

    /** 默认租约 TTL（spec 08）。 */
    public static final Duration DEFAULT_LEASE_TTL = Duration.ofSeconds(90);
    /** 默认心跳间隔（约为 TTL 的 1/3，spec 08）。 */
    public static final Duration DEFAULT_HEARTBEAT_INTERVAL = Duration.ofSeconds(30);
    /** 默认崩溃循环硬顶次数（M1 保守兜底）。 */
    public static final int DEFAULT_CRASHLOOP_HARD_CAP = 3;

    public RecoveryConfig {
        leaseTtl = leaseTtl == null || leaseTtl.isNegative() || leaseTtl.isZero()
                ? DEFAULT_LEASE_TTL : leaseTtl;
        heartbeatInterval = heartbeatInterval == null || heartbeatInterval.isNegative() || heartbeatInterval.isZero()
                ? DEFAULT_HEARTBEAT_INTERVAL : heartbeatInterval;
        durabilityTier = durabilityTier == null ? DurabilityTier.ASYNC : durabilityTier;
        resumeStrategy = resumeStrategy == null ? ResumeStrategy.VOID : resumeStrategy;
        crashloopHardCap = crashloopHardCap <= 0 ? DEFAULT_CRASHLOOP_HARD_CAP : crashloopHardCap;
    }

    /** safe-by-default 全默认（测试 / 兜底用）。 */
    public static RecoveryConfig defaults() {
        return new RecoveryConfig(true, DEFAULT_LEASE_TTL, DEFAULT_HEARTBEAT_INTERVAL,
                DurabilityTier.ASYNC, ResumeStrategy.VOID, DEFAULT_CRASHLOOP_HARD_CAP, true);
    }

    /** 关闭整个机制（回退底座原生行为）。 */
    public static RecoveryConfig disabled() {
        return new RecoveryConfig(false, DEFAULT_LEASE_TTL, DEFAULT_HEARTBEAT_INTERVAL,
                DurabilityTier.ASYNC, ResumeStrategy.VOID, DEFAULT_CRASHLOOP_HARD_CAP, false);
    }
}
