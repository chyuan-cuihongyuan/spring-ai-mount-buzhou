package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryMessageStoreTest {

    private final InMemoryMessageStore store = new InMemoryMessageStore();

    private BuzhouMessage msg(String id, String sessionId, int turnSeq, int seqInTurn) {
        return new BuzhouMessage(id, sessionId, turnSeq, seqInTurn, Role.USER,
                "content-" + id, List.of(), null, null, null, Map.of(), Instant.now());
    }

    @Test
    void loadReturnsMessagesOrderedByTurnAndSeqInTurn() {
        store.append("s1", List.of(msg("m3", "s1", 2, 0)));
        store.append("s1", List.of(msg("m1", "s1", 1, 0), msg("m2", "s1", 1, 1)));

        assertThat(store.load("s1")).extracting(BuzhouMessage::id)
                .containsExactly("m1", "m2", "m3");
    }

    @Test
    void loadIsScopedBySession() {
        store.append("s1", List.of(msg("m1", "s1", 1, 0)));
        store.append("s2", List.of(msg("m2", "s2", 1, 0)));

        assertThat(store.load("s1")).extracting(BuzhouMessage::id).containsExactly("m1");
        assertThat(store.load("unknown")).isEmpty();
    }

    @Test
    void findByIdReturnsMessageForEvidenceLookup() {
        store.append("s1", List.of(msg("m1", "s1", 1, 0)));

        assertThat(store.findById("m1")).isPresent()
                .get().extracting(BuzhouMessage::content).isEqualTo("content-m1");
        assertThat(store.findById("nope")).isEmpty();
    }
}
