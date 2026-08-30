package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 会话优雅排水协调器（spec 155 / T513，K8s pod drain 借鉴）：beginDrain 拒新
 * Turn（SESSION_DRAINING）+ enter 在飞记账（AutoCloseable Lease）+
 * awaitDrained 等排空（<b>超时 false 不死等</b>——升级硬关归调用方决策）。
 *
 * <p>与 SpawnGate.signalDrainStarted（runtime 级停机）正交：本类是单会话维护下线。
 */
public final class SessionDrainCoordinator {

    /** 在飞轮占用（close 归还并触发排空信号）。 */
    public final class Lease implements AutoCloseable {
        private final SessionState state;
        private volatile boolean closed;

        Lease(SessionState state) {
            this.state = state;
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                synchronized (state) {
                    state.inFlight--;
                    if (state.inFlight == 0) {
                        state.drained.countDown();
                    }
                }
            }
        }
    }

    private static final class SessionState {
        volatile boolean draining;
        int inFlight;
        CountDownLatch drained = new CountDownLatch(1);
    }

    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    /** 进入排水态：此后新 Turn 被 SESSION_DRAINING 拒（幂等）。 */
    public void beginDrain(String sessionId) {
        SessionState state = sessions.computeIfAbsent(sessionId, k -> new SessionState());
        synchronized (state) {
            state.draining = true;
            if (state.inFlight == 0) {
                state.drained.countDown(); // 无在飞：即刻排空
            }
        }
    }

    /** 是否排水中。 */
    public boolean isDraining(String sessionId) {
        SessionState state = sessions.get(sessionId);
        return state != null && state.draining;
    }

    /**
     * 取一个在飞轮占用：排水中抛 SESSION_DRAINING（新 Turn 拒绝——可读理由）。
     */
    public Lease enter(String sessionId) {
        SessionState state = sessions.computeIfAbsent(sessionId, k -> new SessionState());
        synchronized (state) {
            if (state.draining) {
                throw new BuzhouException(ErrorCode.SESSION_DRAINING,
                        "会话排水维护中，拒绝新 Turn（sessionId=" + sessionId
                                + "；在飞轮排空后进入归档/下线）");
            }
            state.inFlight++;
            state.drained = new CountDownLatch(1); // 有在飞：排空信号重置
            return new Lease(state);
        }
    }

    /** 等排空：在飞归零 true；超时 false（不死等——升级决策归调用方）。 */
    public boolean awaitDrained(String sessionId, Duration timeout) throws InterruptedException {
        SessionState state = sessions.get(sessionId);
        if (state == null) {
            return true;
        }
        CountDownLatch latch;
        synchronized (state) {
            if (state.inFlight == 0) {
                return true;
            }
            latch = state.drained;
        }
        return latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    /** 排水会话名单。 */
    public Set<String> drainingSessions() {
        Set<String> out = new TreeSet<>();
        sessions.forEach((id, state) -> {
            if (state.draining) {
                out.add(id);
            }
        });
        return out;
    }

    /** 会话在飞轮数（观测面）。 */
    public int inFlightOf(String sessionId) {
        SessionState state = sessions.get(sessionId);
        if (state == null) {
            return 0;
        }
        synchronized (state) {
            return state.inFlight;
        }
    }
}
