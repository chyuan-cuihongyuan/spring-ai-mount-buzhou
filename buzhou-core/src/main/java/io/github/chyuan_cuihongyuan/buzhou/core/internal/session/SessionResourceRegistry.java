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
                if (first == null) {
                    first = new RuntimeException("Failed to close session resource: " + entry.name(), e);
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
