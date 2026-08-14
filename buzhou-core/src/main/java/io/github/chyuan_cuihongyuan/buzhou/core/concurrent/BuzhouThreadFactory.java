package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.concurrent.ThreadFactory;

/**
 * impl-34 / spec 13 §core-4：统一线程工厂——全部 buzhou 线程以 {@code buzhou-<role>-<seq>}
 * 前缀命名（thread dump 可归属）并携带未捕获异常处理器（异常统一 ERROR 日志，绝不蒸发）。
 *
 * <p>源：Kafka（67K★）线程命名约定（{@code kafka-producer-network-thread-...}）。
 * 虚拟线程与平台线程双形态；实现 {@link ThreadFactory}，可直接供
 * {@code Executors.newVirtualThreadPerTaskExecutor(...)} 等消费。
 */
public final class BuzhouThreadFactory implements ThreadFactory {

    private static final System.Logger LOGGER =
            System.getLogger(BuzhouThreadFactory.class.getName());

    private final ThreadFactory delegate;

    private BuzhouThreadFactory(ThreadFactory delegate) {
        this.delegate = delegate;
    }

    /** 虚拟线程工厂（{@code Thread.ofVirtual()}；命名 {@code buzhou-<role>-<seq>}）。 */
    public static BuzhouThreadFactory virtual(String role) {
        return new BuzhouThreadFactory(baseBuilder(role, Thread.ofVirtual()).factory());
    }

    /** 平台线程工厂（{@code Thread.ofPlatform()}；命名 {@code buzhou-<role>-<seq>}）。 */
    public static BuzhouThreadFactory platform(String role) {
        return new BuzhouThreadFactory(baseBuilder(role, Thread.ofPlatform()).factory());
    }

    private static Thread.Builder baseBuilder(String role, Thread.Builder builder) {
        String normalized = role == null || role.isBlank() ? "generic" : role.trim();
        return builder.name("buzhou-" + normalized + "-", 1L)
                .uncaughtExceptionHandler((thread, throwable) -> LOGGER.log(
                        System.Logger.Level.ERROR,
                        "buzhou 线程未捕获异常（thread=" + thread.getName() + "）", throwable));
    }

    @Override
    public Thread newThread(Runnable task) {
        return delegate.newThread(task);
    }
}
