package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 谱系游走导入场景深链补验（spec 740 / T1031–T1032 / impl 543）：100 节点链
 * +尾部环——默认深度截断不 OOM；显式深度走至环处 loopDetected。
 */
class ForkLineageWalkerImportE2ETest {

    private static void source(SessionStateStore store, String session, String source) {
        store.put(session, new StateEntry(SessionForkKeys.SOURCE, source,
                SessionForkKeys.PRODUCER, 0, null, Instant.now()));
    }

    @Test
    void deepChainWithTailLoopCappedThenLoopDetected() {
        InMemorySessionStateStore store = new InMemorySessionStateStore();
        // 100 节点链：s0 → s1 → … → s99，尾部 s99 → s98（环）
        for (int i = 0; i < 99; i++) {
            source(store, "s" + i, "s" + (i + 1));
        }
        source(store, "s99", "s98");

        // 默认深度 64：截断（不 OOM）
        ForkLineageWalker.Lineage capped = ForkLineageWalker.walk(store, "s0");
        assertThat(capped.depthCapped()).isTrue();
        assertThat(capped.loopDetected()).isFalse();
        assertThat(capped.ancestors()).hasSize(64);

        // 显式深度 200：走至尾部环 → loopDetected
        ForkLineageWalker.Lineage looped = ForkLineageWalker.walk(store, "s0", 200);
        assertThat(looped.loopDetected()).isTrue();
        assertThat(looped.ancestors().size()).isLessThan(200);
    }
}
