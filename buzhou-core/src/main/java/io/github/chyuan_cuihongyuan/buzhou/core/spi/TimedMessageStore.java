package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 计时消息存储装饰器（spec 810 / T1121）：包装任意 {@link MessageStore}，
 * append/load/findById 耗时进 {@link StoreLatencyRing}（操作名=方法名）。
 * 行为零变更纯透传（异常照抛——耗时照记）；Clock 注入可测。
 */
public final class TimedMessageStore implements MessageStore {

    private final MessageStore delegate;
    private final StoreLatencyRing ring;

    public TimedMessageStore(MessageStore delegate, StoreLatencyRing ring) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.ring = Objects.requireNonNull(ring, "ring");
    }

    @Override
    public void append(String sessionId, List<BuzhouMessage> messages) {
        long start = System.nanoTime();
        try {
            delegate.append(sessionId, messages);
        } finally {
            ring.record("append", elapsedMillis(start));
        }
    }

    @Override
    public List<BuzhouMessage> load(String sessionId) {
        long start = System.nanoTime();
        try {
            return delegate.load(sessionId);
        } finally {
            ring.record("load", elapsedMillis(start));
        }
    }

    @Override
    public Optional<BuzhouMessage> findById(String messageId) {
        long start = System.nanoTime();
        try {
            return delegate.findById(messageId);
        } finally {
            ring.record("findById", elapsedMillis(start));
        }
    }

    private static long elapsedMillis(long startNanos) {
        return Math.max(0, (System.nanoTime() - startNanos) / 1_000_000);
    }
}
