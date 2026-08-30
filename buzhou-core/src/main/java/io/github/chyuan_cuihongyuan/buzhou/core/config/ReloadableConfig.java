package io.github.chyuan_cuihongyuan.buzhou.core.config;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * 配置热重载原语（spec 163 / T523，Caddy config reload 借鉴）：volatile 引用
 * 原子替换（读侧零锁）+ 单调版本（<b>同值替换也计版本</b>——重放安全，观察方
 * 按版本幂等）+ 订阅/退订（通知在锁外——防死锁）+ 条件 CAS 更新。
 *
 * <p>watcher（文件/配置中心轮询）归宿主——本类只提供「换与通知」。
 */
public final class ReloadableConfig<T> {

    private volatile T current;
    private final AtomicLong version = new AtomicLong();
    private final List<Consumer<T>> listeners = new CopyOnWriteArrayList<>();
    private final Object writeLock = new Object();

    private ReloadableConfig(T initial) {
        if (initial == null) {
            throw new IllegalArgumentException("initial 配置非空（热重载不引入未定义态）");
        }
        this.current = initial;
    }

    public static <T> ReloadableConfig<T> of(T initial) {
        return new ReloadableConfig<>(initial);
    }

    /** 当前值（volatile 读——高频读零锁）。 */
    public T current() {
        return current;
    }

    /** 单调版本号（含同值替换——重放安全）。 */
    public long version() {
        return version.get();
    }

    /** 原子替换：版本自增 + 通知全部订阅者（锁外通知）。 */
    public T replace(T next) {
        if (next == null) {
            throw new IllegalArgumentException("热重载目标非空");
        }
        T previous;
        synchronized (writeLock) {
            previous = current;
            current = next;
            version.incrementAndGet();
        }
        notifyListeners(next);
        return previous;
    }

    /**
     * 条件 CAS 更新：operator 基于当前值算新值；窗口内他方已换则基于新当前值
     * 重算（读到最新为达——不留旧基线决策）。
     */
    public T updateIf(UnaryOperator<T> operator) {
        if (operator == null) {
            throw new IllegalArgumentException("operator 非空");
        }
        T next;
        synchronized (writeLock) {
            next = operator.apply(current);
            if (next == null) {
                throw new IllegalArgumentException("updateIf 产出非空");
            }
            current = next;
            version.incrementAndGet();
        }
        notifyListeners(next);
        return next;
    }

    /** 订阅变更（返回退订句柄——防监听泄漏）。 */
    public Runnable subscribe(Consumer<T> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener 非空");
        }
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    private void notifyListeners(T value) {
        for (Consumer<T> listener : listeners) {
            listener.accept(value);
        }
    }
}
