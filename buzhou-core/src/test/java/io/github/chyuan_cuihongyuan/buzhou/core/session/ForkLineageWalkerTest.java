package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * fork 谱系游走环防护测试（spec 711 / T973–T974 / impl 514）：树有序游走、
 * 环检测、深度封顶、无源空谱系。
 */
class ForkLineageWalkerTest {

    private static void source(SessionStateStore store, String session, String source) {
        store.put(session, new StateEntry(SessionForkKeys.SOURCE, source,
                SessionForkKeys.PRODUCER, 0, null, Instant.now()));
    }

    @Test
    void treeWalksNearestToRoot() {
        InMemorySessionStateStore store = new InMemorySessionStateStore();
        source(store, "leaf", "mid");
        source(store, "mid", "root");
        // root 无 SOURCE

        ForkLineageWalker.Lineage lineage = ForkLineageWalker.walk(store, "leaf");

        assertThat(lineage.ancestors()).containsExactly("mid", "root");
        assertThat(lineage.loopDetected()).isFalse();
        assertThat(lineage.depthCapped()).isFalse();
    }

    @Test
    void cycleDetectedAndTerminates() {
        InMemorySessionStateStore store = new InMemorySessionStateStore();
        source(store, "a", "b");
        source(store, "b", "a"); // 环

        ForkLineageWalker.Lineage lineage = ForkLineageWalker.walk(store, "a");

        assertThat(lineage.loopDetected()).isTrue();
        assertThat(lineage.ancestors()).containsExactly("b"); // 重复访问前只记一次
    }

    @Test
    void selfLoopDetected() {
        InMemorySessionStateStore store = new InMemorySessionStateStore();
        source(store, "a", "a"); // 自环（源=自身）

        ForkLineageWalker.Lineage lineage = ForkLineageWalker.walk(store, "a");

        assertThat(lineage.loopDetected()).isTrue();
        assertThat(lineage.ancestors()).isEmpty();
    }

    @Test
    void depthCapTriggers() {
        InMemorySessionStateStore store = new InMemorySessionStateStore();
        source(store, "s5", "s4");
        source(store, "s4", "s3");
        source(store, "s3", "s2");
        source(store, "s2", "s1");
        source(store, "s1", "root");

        ForkLineageWalker.Lineage lineage = ForkLineageWalker.walk(store, "s5", 2);

        assertThat(lineage.depthCapped()).isTrue();
        assertThat(lineage.ancestors()).hasSize(2);
        assertThat(lineage.loopDetected()).isFalse();
    }

    @Test
    void sourcelessSessionYieldsEmptyLineage() {
        InMemorySessionStateStore store = new InMemorySessionStateStore();

        ForkLineageWalker.Lineage lineage = ForkLineageWalker.walk(store, "lonely");

        assertThat(lineage.ancestors()).isEmpty();
        assertThat(lineage.loopDetected()).isFalse();
        assertThat(lineage.depthCapped()).isFalse();
    }
}
