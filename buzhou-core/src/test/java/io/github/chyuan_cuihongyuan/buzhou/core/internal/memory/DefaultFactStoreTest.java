package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.Fact;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.FactStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultFactStoreTest {

    @Test
    void saveAndRetrieveActiveFact() {
        FactStore store = new DefaultFactStore(new InMemorySessionStateStore());
        Fact fact = new Fact(Fact.keyFor("risk", "table-1"), Map.of("risk", "high"), "risk", 1, 3);
        store.save("s1", fact);

        List<Fact> active = store.activeFacts("s1", 1);
        assertThat(active).hasSize(1);
        assertThat(active.get(0).producer()).isEqualTo("risk");
        assertThat(active.get(0).value()).isEqualTo(Map.of("risk", "high"));
    }

    @Test
    void ttlExpiresAfterWindow() {
        FactStore store = new DefaultFactStore(new InMemorySessionStateStore());
        store.save("s1", new Fact(Fact.keyFor("p", "f"), "v", "p", 1, 2));

        // turn 1 (created): active (1-1=0 < 2)
        assertThat(store.activeFacts("s1", 1)).hasSize(1);
        // turn 2: active (2-1=1 < 2)
        assertThat(store.activeFacts("s1", 2)).hasSize(1);
        // turn 3: expired (3-1=2, not < 2)
        assertThat(store.activeFacts("s1", 3)).isEmpty();
    }

    @Test
    void ttlOneIsOneShot() {
        FactStore store = new DefaultFactStore(new InMemorySessionStateStore());
        store.save("s1", new Fact(Fact.keyFor("p", "once"), "v", "p", 5, 1));

        // turn 5 (created): active (5-5=0 < 1)
        assertThat(store.activeFacts("s1", 5)).hasSize(1);
        // turn 6: expired (6-5=1, not < 1)
        assertThat(store.activeFacts("s1", 6)).isEmpty();
    }

    @Test
    void onlyFactNamespaceReturned() {
        FactStore store = newStoreWithState();
        // 非 fact.* 的 state key 不应被返回
        assertThat(store.activeFacts("s1", 1)).hasSize(1);
    }

    @Test
    void deleteRemovesFact() {
        FactStore store = new DefaultFactStore(new InMemorySessionStateStore());
        store.save("s1", new Fact(Fact.keyFor("p", "f"), "v", "p", 1, 5));
        store.delete("s1", Fact.keyFor("p", "f"));
        assertThat(store.activeFacts("s1", 1)).isEmpty();
    }

    @Test
    void multipleFactsSortedByCreatedTurn() {
        FactStore store = new DefaultFactStore(new InMemorySessionStateStore());
        store.save("s1", new Fact(Fact.keyFor("p", "late"), "v2", "p", 3, 5));
        store.save("s1", new Fact(Fact.keyFor("p", "early"), "v1", "p", 1, 5));
        List<Fact> active = store.activeFacts("s1", 4);
        assertThat(active).hasSize(2);
        assertThat(active.get(0).createdTurn()).isEqualTo(1);
        assertThat(active.get(1).createdTurn()).isEqualTo(3);
    }

    private FactStore newStoreWithState() {
        InMemorySessionStateStore state = new InMemorySessionStateStore();
        state.put("s1", new io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry(
                "auth.run_command.abc", "approved", "guard", 1, null, java.time.Instant.now()));
        FactStore store = new DefaultFactStore(state);
        store.save("s1", new Fact(Fact.keyFor("p", "f"), "v", "p", 1, 5));
        return store;
    }
}
