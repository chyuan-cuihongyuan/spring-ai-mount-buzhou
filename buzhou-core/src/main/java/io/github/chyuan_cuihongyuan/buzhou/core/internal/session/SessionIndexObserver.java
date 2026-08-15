package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAssemblyCustomizer;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionObserver;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 会话索引观察者（spec 30 / T109 / impl-84）：生命周期点维护索引——onOpen 入库 ACTIVE、
 * onTurnEnd 刷 lastActive/turnCount、onClose 置 CLOSED。最终一致：更新失败只 WARN
 * 不阻断会话（索引是查询优化面，权威数据在五 store）。
 *
 * <p>经 {@link #wiring(SessionIndexStore)} 产出 {@link SessionAssemblyCustomizer} 挂进
 * runtime 装配（core auto-config 检测到 SessionIndexStore bean 时自动接线；编程式用户
 * 可自行并入 RuntimeConfig.assemblyCustomizers）。
 */
public final class SessionIndexObserver implements SessionObserver {

    private static final System.Logger LOGGER = System.getLogger(SessionIndexObserver.class.getName());

    private final SessionIndexStore index;
    private final String sessionId;
    private final String appId;
    private final String agentName;
    private final Map<String, String> tags;
    private final long createdAt = System.currentTimeMillis();
    private final AtomicInteger turns = new AtomicInteger();
    private volatile long lastActiveAt = createdAt;

    public SessionIndexObserver(SessionIndexStore index, String appId, String agentName,
            String sessionId, Map<String, String> tags) {
        this.index = index;
        this.sessionId = sessionId;
        this.appId = appId;
        this.agentName = agentName;
        this.tags = tags == null ? Map.of() : Map.copyOf(tags);
    }

    /** 装配定制器工厂（每会话注册一个观察者实例）。 */
    public static SessionAssemblyCustomizer wiring(SessionIndexStore index) {
        return ctx -> ctx.addObserver(new SessionIndexObserver(
                index, ctx.appId(), ctx.agentName(), ctx.sessionId(), Map.of()));
    }

    @Override
    public void onOpen() {
        upsertQuietly(SessionInfo.STATUS_ACTIVE);
        maybePurge();
    }

    /**
     * spec 37 §C / T134：保留策略惰性清扫——1/64 概率触发、单次上限 256 条
     * （免热路径开销；retention 由装配方经静态配置注入，-1 = 永不清扫）。
     */
    private void maybePurge() {
        java.time.Duration retention = RETENTION;
        if (retention == null || retention.isNegative() || retention.isZero()) {
            return; // -1/0 = 永久保留
        }
        if (PURGE_RANDOM.nextInt(64) != 0) {
            return;
        }
        try {
            index.purgeOlderThan(java.time.Instant.now().minus(retention), 256);
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.WARNING, "索引保留清扫失败（忽略，下次再试）：" + e.getMessage());
        }
    }

    /** 保留期（静态装配配置：buzhou.index.closed-retention；null/-1 = 永久）。 */
    private static volatile java.time.Duration RETENTION;
    private static final java.util.Random PURGE_RANDOM = new java.util.Random();

    /** 装配期注入保留期（auto-config / 编程式调用一次）。 */
    public static void configureRetention(java.time.Duration retention) {
        RETENTION = retention;
    }

    @Override
    public void onTurnEnd(int turnSeq, String finalReply) {
        lastActiveAt = System.currentTimeMillis();
        turns.incrementAndGet();
        upsertQuietly(SessionInfo.STATUS_ACTIVE);
    }

    @Override
    public void onClose() {
        lastActiveAt = System.currentTimeMillis();
        upsertQuietly(SessionInfo.STATUS_CLOSED);
    }

    private void upsertQuietly(String status) {
        try {
            index.upsert(new SessionInfo(sessionId, appId, agentName, status,
                    createdAt, lastActiveAt, turns.get(), tags));
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "会话索引更新失败（最终一致口径，不阻断会话；sessionId=" + sessionId + "）：" + e.getMessage());
        }
    }
}
