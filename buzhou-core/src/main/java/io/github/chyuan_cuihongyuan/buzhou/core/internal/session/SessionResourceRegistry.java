package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class SessionResourceRegistry {

    private record Entry(String name, AutoCloseable resource) {
    }

    private final List<Entry> entries = new CopyOnWriteArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    public void register(String name, AutoCloseable resource) {
        if (closed.get()) {
            throw new IllegalStateException("Registry already closed, cannot register: " + name);
        }
        entries.add(new Entry(name, resource));
    }

    /**
     * 逆序（LIFO）关闭全部已注册资源。
     *
     * <p>ticket 29 日志基线：单个资源 close 失败<b>不跳过其余清理</b>——全部尝试完毕后抛出
     * 首个失败，其余失败以 suppressed 附加（绝不静默吞）。
     */
    public void closeAll() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        List<Entry> reversed = entries.reversed();
        RuntimeException first = null;
        for (Entry entry : reversed) {
            try {
                entry.resource().close();
            } catch (Exception e) {
                RuntimeException failure =
                        new RuntimeException("Failed to close session resource: " + entry.name(), e);
                if (first == null) {
                    first = failure;
                } else {
                    first.addSuppressed(failure);
                }
            }
        }
        if (first != null) {
            throw first;
        }
    }

    public boolean isClosed() {
        return closed.get();
    }
}
