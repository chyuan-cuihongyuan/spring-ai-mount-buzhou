package io.github.chyuan_cuihongyuan.buzhou.core.session;

/**
 * 实例容量超限异常（spec「背压与多层限流 · 维度① spawn 闸」）。
 *
 * <p>当实例活跃会话数达到 {@code buzhou.backpressure.max-concurrent-sessions} 上限且过载策略为
 * {@link io.github.chyuan_cuihongyuan.buzhou.core.backpressure.OverloadPolicy#FAIL_FAST FAIL_FAST}，
 * 或排队超时（{@link OverloadPolicy#QUEUE QUEUE} 档等待 {@code spawn-queue-timeout} 后仍无空位）时抛出。
 *
 * <p>参照 {@link RuntimeDrainingException} 形态：独立异常类型、{@code RuntimeException} 子类，
 * 调用方可按类型分流——仅对容量拒绝做重试 / 路由，对 drain 拒绝走另一路径。
 *
 * <p>message 带 sessionId + 当前活跃数 + 上限 + 已等待时长，便于排障（异常规约）。
 */
public class SessionCapacityExceededException extends RuntimeException {

    private final String sessionId;
    private final int currentCount;
    private final int limit;
    private final long waitedMillis;

    /**
     * @param sessionId     被拒绝的会话 id
     * @param currentCount  拒绝时实例活跃会话数
     * @param limit         配置的活跃会话上限
     * @param waited        排队已等待时长（FAIL_FAST 档为 {@link java.time.Duration#ZERO}）
     */
    public SessionCapacityExceededException(String sessionId, int currentCount, int limit,
                                            java.time.Duration waited) {
        super("Session capacity exceeded: sessionId=" + sessionId
                + ", currentActive=" + currentCount
                + ", limit=" + limit
                + ", waitedMs=" + (waited == null ? 0 : waited.toMillis()));
        this.sessionId = sessionId;
        this.currentCount = currentCount;
        this.limit = limit;
        this.waitedMillis = waited == null ? 0 : waited.toMillis();
    }

    public String sessionId() {
        return sessionId;
    }

    public int currentCount() {
        return currentCount;
    }

    public int limit() {
        return limit;
    }

    public long waitedMillis() {
        return waitedMillis;
    }
}
