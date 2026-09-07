package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaderElector;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 进程内选主实现（spec 331 / T653）：单实例语义——try 即 leader、
 * resign 即空位；纪元首取为 1，「空位后再取」递增（续期不变）——
 * 单状态原子交换保证 holder/epoch 一致。多实例无共识，仅供单实例
 * 部署/测试/演示；跨实例选主用 store-redis 的 RedisLeaderElector。
 */
public final class InMemoryLeaderElector implements LeaderElector {

    private record State(String holder, long epoch) {
    }

    private final String holderId;
    private final AtomicReference<State> state = new AtomicReference<>(new State(null, 0));

    public InMemoryLeaderElector(String holderId) {
        this.holderId = holderId == null || holderId.isBlank()
                ? "in-memory-" + ProcessHandle.current().pid() : holderId;
    }

    @Override
    public Leadership tryAcquireOrRenew() {
        State result = state.updateAndGet(s -> {
            if (holderId.equals(s.holder())) {
                return s; // 续期：持有人与纪元都不变
            }
            if (s.holder() == null) {
                return new State(holderId, s.epoch() + 1); // 首取/空位再取：进入新纪元
            }
            return s; // 他人持有：不动
        });
        return new Leadership(result.holder(), result.epoch(),
                holderId.equals(result.holder()));
    }

    @Override
    public void resign() {
        state.updateAndGet(s -> holderId.equals(s.holder())
                ? new State(null, s.epoch()) : s); // 仅持有人让位；纪元留给下任递增
    }

    @Override
    public Leadership inspect() {
        State current = state.get();
        return new Leadership(current.holder(), current.epoch(),
                holderId.equals(current.holder()));
    }
}
