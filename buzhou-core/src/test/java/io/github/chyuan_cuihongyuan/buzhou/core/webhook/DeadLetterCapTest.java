package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-689 / spec 937：死信环形上限——容量 256 封顶、超限丢最旧（createdAt
 * 升序）、保留最新、总量不超上限。
 */
class DeadLetterCapTest {

    @Test
    void deadLetterStoreCappedAtMax() {
        SessionStateStore store = new InMemorySessionStateStore();
        WebhookOutbox outbox = new WebhookOutbox(store, 64);

        // 超限制造死信：上限 256，投 260 条
        for (int i = 0; i < 260; i++) {
            // seq 单调 → createdAt 序（ev-0 最旧）
            outbox.markDead(new WebhookOutbox.OutboxRecord("ev-" + i, "test.type",
                    "payload-" + i, i, 3, 0L, i));
        }

        List<WebhookDeadLetter> dead = outbox.deadLetters(Integer.MAX_VALUE);
        assertThat(dead).hasSize(256); // 环形封顶
        // 丢最旧：ev-0..ev-3 被逐出，保留 ev-4..ev-259
        assertThat(dead.stream().noneMatch(d -> d.eventId().equals("ev-0"))).isTrue();
        assertThat(dead.stream().noneMatch(d -> d.eventId().equals("ev-259"))).isFalse();
        assertThat(outbox.deadLetters(500).size()).isEqualTo(256);
    }
}
