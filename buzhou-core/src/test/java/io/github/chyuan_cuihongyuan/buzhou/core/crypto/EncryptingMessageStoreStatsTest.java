package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptingMessageStoreStatsTest {

    private static EnvelopeCipher cipher() {
        byte[] key = new byte[32];
        java.util.Arrays.fill(key, (byte) 7);
        return new EnvelopeCipher(Base64.getEncoder().encodeToString(key), null);
    }

    private static BuzhouMessage message(String sessionId, String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, 2, 1,
                Role.ASSISTANT, content,
                List.of(new ToolCallRecord("tc-1", "echo", "{\"a\":1}")),
                "tc-1", "推理内容", "sig-blob",
                Map.of("k", "v", "n", 42), Instant.parse("2026-09-04T12:00:00Z"));
    }

    @Test
    void appendAndLoadCountEncryptedAndDecrypted() {
        MessageStore backing = Buzhou.inMemoryStores().messageStore();
        EncryptingMessageStore store = new EncryptingMessageStore(backing, cipher());

        store.append("s-1", List.of(message("s-1", "甲"), message("s-1", "乙")));
        store.load("s-1");

        EncryptingMessageStore.CryptoStoreStats stats = store.stats();
        assertThat(stats.encrypted()).isEqualTo(2);
        assertThat(stats.decrypted()).isEqualTo(2);
        assertThat(stats.passthrough()).isZero();
    }

    @Test
    void legacyPlaintextLoadCountsPassthrough() {
        MessageStore backing = Buzhou.inMemoryStores().messageStore();
        EncryptingMessageStore store = new EncryptingMessageStore(backing, cipher());
        backing.append("s-old", List.of(message("s-old", "旧明文")));

        store.load("s-old");

        assertThat(store.stats().passthrough()).isEqualTo(1);
        assertThat(store.stats().decrypted()).isZero();
    }

    @Test
    void alreadyEnvelopeAppendCountsPassthrough() {
        MessageStore backing = Buzhou.inMemoryStores().messageStore();
        EncryptingMessageStore store = new EncryptingMessageStore(backing, cipher());
        store.append("s-1", List.of(message("s-1", "首次")));
        List<BuzhouMessage> carriers = backing.load("s-1");

        store.append("s-1", carriers); // 已是信封——幂等跳过

        EncryptingMessageStore.CryptoStoreStats stats = store.stats();
        assertThat(stats.encrypted()).isEqualTo(1);
        assertThat(stats.passthrough()).isEqualTo(1);
    }

    @Test
    void deleteSessionCountsNothing() {
        MessageStore backing = Buzhou.inMemoryStores().messageStore();
        EncryptingMessageStore store = new EncryptingMessageStore(backing, cipher());

        store.deleteSession("s-1");

        assertThat(store.stats()).isEqualTo(new EncryptingMessageStore.CryptoStoreStats(0, 0, 0));
    }
}
