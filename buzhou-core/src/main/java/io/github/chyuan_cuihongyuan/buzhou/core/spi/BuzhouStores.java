package io.github.chyuan_cuihongyuan.buzhou.core.spi;
/**
 * 持久化聚合 record——消息 / 摘要 / 会话状态 / 租约 / 观测五 SPI 的成套载体
 * （内存默认实现由 Buzhou.inMemoryStores 提供）。
 */
public record BuzhouStores(
        MessageStore messageStore,
        SummaryStore summaryStore,
        SessionStateStore sessionStateStore,
        SessionLeaseStore sessionLeaseStore,
        ObservabilityStore observabilityStore,
        UnitOfWork unitOfWork) {
}
