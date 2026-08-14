package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.session.LeaseLostException;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaseInfo;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionLeaseStore;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * impl-33 / spec 13 §core-3：单会话租约持有哨兵——续租双路径（Turn 轮间 + 后台 TTL/3）与
 * 写路径 fence 的统一裁决点。持有 {@code (sessionId, ownerId, fencingToken)} 三元组与本地
 * 过期时刻估计，把「续租 / fence 校验 / 丢失标记」收敛为一个线程安全对象：
 *
 * <ul>
 *   <li>{@link #beforeRound()}：Turn 循环每轮开始（工具结果落库前）——先 fence（inspect 校验
 *       fencingToken 仍持有，双主窗口零写入），剩余租期 &lt; 阈值时续租；任一失败即抛
 *       {@link LeaseLostException}；</li>
 *   <li>{@link #renewQuietly()}：后台调度线程（TTL/3 节奏无条件续租；失败仅标记丢失，
 *       调度线程不抛）；</li>
 *   <li>{@link #checkFence()}：Turn 收尾提交点（Completed-Turn 快照写入前）的 fence 校验。</li>
 * </ul>
 *
 * <p>过期时刻为<b>本地估计</b>（acquire/renew 成功时刻 + TTL，store 往返偏差可忽略）——它只
 * 决定「何时该续租」的节奏；正确性完全由 store 侧 {@code renew/inspect} 的 fencingToken
 * 校验保证（估计偏晚最多导致一次注定失败的续租 → LeaseLost，安全方向失效）。
 */
public final class SessionLeaseGuard {

    private final SessionLeaseStore store;
    private final String sessionId;
    private final String ownerId;
    private final long fencingToken;
    private final Duration ttl;
    private final Duration renewThreshold;
    private final AtomicBoolean lost = new AtomicBoolean();
    private final AtomicInteger renewals = new AtomicInteger();
    private volatile Instant expiresAt;
    private final io.github.chyuan_cuihongyuan.buzhou.core.leak.ResourceLeakDetector.LeakHandle leakHandle;

    public SessionLeaseGuard(SessionLeaseStore store, String sessionId, String ownerId,
                             long fencingToken, Duration ttl, Duration renewThreshold) {
        this.store = store;
        this.sessionId = sessionId;
        this.ownerId = ownerId;
        this.fencingToken = fencingToken;
        this.ttl = ttl;
        this.renewThreshold = renewThreshold;
        this.expiresAt = Instant.now().plus(ttl);
        // impl-41 / spec 13 §T66：租约挂点——未释放（markLost/close）即被 GC = 泄漏嫌疑
        this.leakHandle = io.github.chyuan_cuihongyuan.buzhou.core.leak.LeakDetectorHolder
                .detector().track("lease:" + sessionId);
    }

    /**
     * Turn 轮间裁决（每轮开始、工具结果落库<b>前</b>调用）：fence 校验 + 剩余不足时续租。
     * 租约已被 steal / 过期不可再取 → 抛 {@link LeaseLostException}（调用方据此中止 Turn，
     * 在飞工具结果不入历史）。
     */
    public void beforeRound() {
        if (lost.get()) {
            throw lostException();
        }
        checkFence();
        renewIfDue();
    }

    /** 剩余租期 &lt; 阈值时续租（省 store 往返）；失败即 LeaseLost。 */
    public void renewIfDue() {
        if (lost.get()) {
            throw lostException();
        }
        if (remaining().compareTo(renewThreshold) > 0) {
            return;
        }
        renewOrLose();
    }

    /**
     * 后台续租（TTL/3 节奏无条件续租，使剩余租期恒 ≥ 2/3 TTL）：失败（被 steal / 过期不可
     * 再取）仅标记丢失并返回 false——调度线程不抛异常；在途 Turn 由轮间 fence / 提交点 fence
     * 兜底中止，后续 chat() 由会话层以明确错误拒绝。
     */
    public boolean renewQuietly() {
        if (lost.get()) {
            return false;
        }
        if (store.renew(sessionId, ownerId, fencingToken, ttl)) {
            expiresAt = Instant.now().plus(ttl);
            renewals.incrementAndGet();
            return true;
        }
        lost.set(true);
        leakHandle.close();
        return false;
    }

    /**
     * 写路径 fence：校验 {@code inspect(sessionId)} 的 fencingToken 仍与本会话持有的一致。
     * 不一致（被 steal）或租约不存在（过期已被惰性移除）→ 标记丢失并抛
     * {@link LeaseLostException}——双主窗口内本地零写入。
     */
    public void checkFence() {
        if (lost.get()) {
            throw lostException();
        }
        Optional<LeaseInfo> current = store.inspect(sessionId);
        if (current.isEmpty() || current.get().fencingToken() != fencingToken) {
            throw markLostAndThrow();
        }
    }

    /** 显式标记租约已丢失（会话层捕获 LeaseLost 后回写，保证后续调用即刻拒绝）。 */
    public void markLost() {
        lost.set(true);
        leakHandle.close(); // 租约生命周期结束（丢失也是结束——不再泄漏嫌疑）
    }

    /** impl-41：会话正常收尾释放（幂等；泄漏登记解除）。 */
    public void close() {
        leakHandle.close();
    }

    public boolean isLost() {
        return lost.get();
    }

    public String sessionId() {
        return sessionId;
    }

    public long fencingToken() {
        return fencingToken;
    }

    /** 成功续租次数（测试可观测性）。 */
    public int renewalCount() {
        return renewals.get();
    }

    private void renewOrLose() {
        if (store.renew(sessionId, ownerId, fencingToken, ttl)) {
            expiresAt = Instant.now().plus(ttl);
            renewals.incrementAndGet();
            return;
        }
        throw markLostAndThrow();
    }

    private Duration remaining() {
        return Duration.between(Instant.now(), expiresAt);
    }

    private LeaseLostException markLostAndThrow() {
        lost.set(true);
        leakHandle.close();
        return new LeaseLostException(sessionId);
    }

    private LeaseLostException lostException() {
        return new LeaseLostException(sessionId);
    }
}
