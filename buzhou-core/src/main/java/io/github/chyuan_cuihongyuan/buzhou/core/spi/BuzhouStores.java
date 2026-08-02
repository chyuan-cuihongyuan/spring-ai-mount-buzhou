package io.github.chyuan_cuihongyuan.buzhou.core.spi;

public record BuzhouStores(
        MessageStore messageStore,
        SummaryStore summaryStore,
        SessionStateStore sessionStateStore,
        SessionLeaseStore sessionLeaseStore,
        ObservabilityStore observabilityStore,
        UnitOfWork unitOfWork) {
}
