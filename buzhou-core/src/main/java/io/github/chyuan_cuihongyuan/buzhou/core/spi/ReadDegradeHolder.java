package io.github.chyuan_cuihongyuan.buzhou.core.spi;

/**
 * 读降级策略全局默认持有器（spec 42 §B / T156 / impl-127；Holder 模式同
 * {@code ToolResultLimiterHolder}）：装配期由 auto-config 按属性写入，进程内全
 * {@code BuzhouChatMemory} 实例共享；测试可临时改写（try/finally 复原）。
 *
 * @since 1.0.0
 */
public final class ReadDegradeHolder {

    private static volatile ReadDegradePolicy policy = ReadDegradePolicy.OFF;

    private ReadDegradeHolder() {
    }

    public static ReadDegradePolicy get() {
        return policy;
    }

    public static void set(ReadDegradePolicy value) {
        policy = value == null ? ReadDegradePolicy.OFF : value;
    }
}
