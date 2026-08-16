package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.RateLimitBackend;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Redis 固定窗限流后端（spec 54 §B / T223 / effort#14）：分钟窗 INCR/DECR + 首写 EXPIRE
 * （LiteLLM Router per-deployment rpm/tpm 同款）——多实例共享额度（总闸正确）。
 *
 * <p><b>诚实差异</b>（vs 内存令牌桶）：固定窗在窗口边界有 2× 尖峰可能（两窗相接处理翻倍
 * 速率）；额度总量与拒绝语义两档等价。TPM 记账可致窗口计数超容量（负余额在窗口键数值上
 * 表达，下窗自然重置）。
 *
 * <p>故障语义：Redis 不可达即 fail-fast 上抛（STORE_WRITE_FAILED，带修法）——
 * <b>不静默 fail-open</b>（限流失效比暂不可用更危险）。
 */
public final class RedisRateLimitBackend implements RateLimitBackend, AutoCloseable {

    private static final String DIMENSION_RPM = "RPM";
    private static final String DIMENSION_TPM = "TPM";

    /** 窗口键 TTL（61s = 分钟窗 + 1s 覆盖时差）。 */
    private static final long WINDOW_TTL_SECONDS = 61;

    private final StatefulRedisConnection<String, String> connection;
    private final String keyPrefix;
    private final double rpmCapacity;
    private final double tpmCapacity;
    private final RedisCommands<String, String> commands;

    /**
     * @param client    Lettuce 客户端（本类独占派生连接；调用方拥有 client 生命周期，
     *                  client.shutdown() 会一并关闭本连接）
     * @param keyPrefix 键前缀（默认 {@code buzhou:}，与 store 同域隔离建议带 {@code rl:} 段）
     * @param rpm       RPM 容量（null/≤0 = 维度关闭）
     * @param tpm       TPM 容量（null/≤0 = 维度关闭）
     */
    public RedisRateLimitBackend(RedisClient client, String keyPrefix, Integer rpm, Integer tpm) {
        this(client.connect(), keyPrefix, rpm, tpm);
    }

    RedisRateLimitBackend(StatefulRedisConnection<String, String> connection, String keyPrefix,
            Integer rpm, Integer tpm) {
        this.connection = connection;
        this.keyPrefix = keyPrefix == null || keyPrefix.isBlank() ? "buzhou:rl:" : keyPrefix;
        this.rpmCapacity = rpm != null && rpm > 0 ? rpm : 0;
        this.tpmCapacity = tpm != null && tpm > 0 ? tpm : 0;
        this.commands = connection.sync();
    }

    /** 连接生命周期出口（宿主显式关闭；client.shutdown() 亦可覆盖）。 */
    public void close() {
        connection.close();
    }

    @Override
    public boolean tryAcquire(String modelName, String dimension, double amount) {
        double cap = capacity(dimension);
        if (cap <= 0) {
            return true; // 维度未启用（limiter 层也不会路由到此，双保险）
        }
        String key = windowKey(modelName, dimension);
        try {
            if (amount <= 0) {
                return available(modelName, dimension) > 0;
            }
            long newCount = commands.incrby(key, (long) Math.ceil(amount));
            if (newCount <= (long) Math.ceil(cap)) {
                touchTtl(key, newCount, amount);
                return true;
            }
            // 超限回滚（固定窗 INCR-then-rollback 模式；竞争窗口内瞬时偏差诚实入档）
            commands.decrby(key, (long) Math.ceil(amount));
            return false;
        } catch (RuntimeException e) {
            throw redisFailure(e);
        }
    }

    @Override
    public void consume(String modelName, String dimension, double amount) {
        double cap = capacity(dimension);
        if (cap <= 0 || amount <= 0) {
            return;
        }
        String key = windowKey(modelName, dimension);
        try {
            long newCount = commands.incrby(key, (long) Math.ceil(amount));
            touchTtl(key, newCount, amount);
        } catch (RuntimeException e) {
            throw redisFailure(e);
        }
    }

    @Override
    public double available(String modelName, String dimension) {
        double cap = capacity(dimension);
        if (cap <= 0) {
            return 0;
        }
        try {
            String raw = commands.get(windowKey(modelName, dimension));
            long count = raw == null ? 0 : Long.parseLong(raw);
            return Math.max(0, cap - count);
        } catch (RuntimeException e) {
            throw redisFailure(e);
        }
    }

    @Override
    public double capacity(String dimension) {
        return DIMENSION_RPM.equals(dimension) ? rpmCapacity
                : DIMENSION_TPM.equals(dimension) ? tpmCapacity : 0;
    }

    /** 固定窗语义：等待 = 当前窗剩余时间（下一窗全量重置）。 */
    @Override
    public double secondsUntilAvailable(String modelName, String dimension, double amount) {
        long millisIntoWindow = System.currentTimeMillis() % 60_000;
        double wait = (60_000 - millisIntoWindow) / 1000.0;
        return wait + ThreadLocalRandom.current().nextDouble(0, 0.05); // 微抖动防多实例同相位
    }

    @Override
    public String kind() {
        return "redis";
    }

    // ---- 键与故障语义 ----

    private String windowKey(String modelName, String dimension) {
        long window = System.currentTimeMillis() / 60_000;
        return keyPrefix + sanitize(modelName) + ":" + dimension + ":" + window;
    }

    private static String sanitize(String modelName) {
        return modelName == null ? "unknown"
                : modelName.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private void touchTtl(String key, long newCount, double amount) {
        if (Math.ceil(amount) >= newCount) {
            // 首写（newCount == amount）：设窗口 TTL
            commands.expire(key, WINDOW_TTL_SECONDS);
        }
    }

    private static BuzhouException redisFailure(RuntimeException cause) {
        return new BuzhouException(ErrorCode.STORE_WRITE_FAILED,
                "Redis 限流后端不可达（fail-fast，不降级 fail-open）：" + cause.getMessage()
                        + "（修法：检查 Redis 连通性与 buzhou.store.* 地址配置；共享限流依赖存储可用性，"
                        + "恢复前调用按错误处置——宁拒不静默放行）",
                cause);
    }
}
