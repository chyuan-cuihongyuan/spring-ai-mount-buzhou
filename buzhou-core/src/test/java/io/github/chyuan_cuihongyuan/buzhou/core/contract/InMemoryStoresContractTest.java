package io.github.chyuan_cuihongyuan.buzhou.core.contract;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;

class InMemoryStoresContractTest extends AbstractBuzhouStoresContractTest {

    private final BuzhouStores stores = Buzhou.inMemoryStores();

    @Override
    protected BuzhouStores stores() {
        return stores;
    }

    @Override
    protected void cleanUp() {
    }
}
