package io.github.chyuan_cuihongyuan.buzhou.core.exec;

/**
 * impl-760 / spec 1007：凭证租约生命周期只读快照（Vault lease lifecycle 借鉴——
 * 续租拒绝率是 TTL 配置健康度的标准信号）。
 *
 * @param issued        累计签发数（同名重签覆盖亦计）
 * @param expired       累计过期数（resolve 惰性剔除 + activeLeases 清扫）
 * @param revoked       累计吊销数（幂等吊销只计一次）
 * @param renewed       累计续租成功数
 * @param renewRejected 累计续租被拒数（租约缺失或已过期满拒）
 */
public record SecretLeaseStats(long issued, long expired, long revoked,
        long renewed, long renewRejected) {
}
