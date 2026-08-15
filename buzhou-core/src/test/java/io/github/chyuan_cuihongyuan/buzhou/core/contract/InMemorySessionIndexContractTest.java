package io.github.chyuan_cuihongyuan.buzhou.core.contract;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionIndexStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore;
import org.junit.jupiter.api.AfterEach;

/** 内存实现契约接入（重启重建语义：进程内无持久断言——见 spec 33 §A 降级口径）。 */
class InMemorySessionIndexContractTest extends AbstractSessionIndexContractTest {

    private InMemorySessionIndexStore store = new InMemorySessionIndexStore();

    @Override
    protected SessionIndexStore index() {
        return store;
    }

    @Override
    @AfterEach
    protected void cleanUp() {
        store = new InMemorySessionIndexStore();
    }
}
