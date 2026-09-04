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

/**
 * spec 333 / impl-356：消息加密装饰器回归——底层只见载体（信封+role 占位）、
 * load/findById 全字段还原（含 toolCalls/metadata/createdAt）、旧明文透传、
 * 已是信封不再包装、deleteSession 直通。
 */
class EncryptingMessageStoreTest {

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
    void underlyingStoreSeesOnlyCarrier_plaintextNeverAtRest() {
        MessageStore backing = Buzhou.inMemoryStores().messageStore();
        EncryptingMessageStore store = new EncryptingMessageStore(backing, cipher());
        store.append("s-1", List.of(message("s-1", "绝密内容")));

        List<BuzhouMessage> atRest = backing.load("s-1");
        assertThat(atRest).hasSize(1);
        BuzhouMessage carrier = atRest.get(0);
        assertThat(carrier.content()).startsWith("buzhou:v1:").doesNotContain("绝密");
        assertThat(carrier.role()).isEqualTo(Role.USER); // 占位——真 role 在密文内
        assertThat(carrier.reasoningContent()).isNull(); // 敏感字段不落明文
        assertThat(carrier.toolCalls()).isEmpty();
        assertThat(carrier.createdAt()).isEqualTo(Instant.parse("2026-09-04T12:00:00Z")); // 排序路由保留
    }

    @Test
    void loadRestoresAllFields() {
        EncryptingMessageStore store = new EncryptingMessageStore(
                Buzhou.inMemoryStores().messageStore(), cipher());
        BuzhouMessage original = message("s-1", "内容");
        store.append("s-1", List.of(original));

        BuzhouMessage restored = store.load("s-1").get(0);
        assertThat(restored.id()).isEqualTo(original.id());
        assertThat(restored.role()).isEqualTo(Role.ASSISTANT);
        assertThat(restored.content()).isEqualTo("内容");
        assertThat(restored.reasoningContent()).isEqualTo("推理内容");
        assertThat(restored.reasoningSignature()).isEqualTo("sig-blob");
        assertThat(restored.toolCallId()).isEqualTo("tc-1");
        assertThat(restored.toolCalls()).hasSize(1);
        assertThat(restored.toolCalls().get(0).name()).isEqualTo("echo");
        assertThat(restored.toolCalls().get(0).arguments()).isEqualTo("{\"a\":1}");
        assertThat(restored.metadata()).containsEntry("k", "v").containsEntry("n", 42);
        assertThat(restored.createdAt()).isEqualTo(original.createdAt());
    }

    @Test
    void findByIdRoundTrips() {
        EncryptingMessageStore store = new EncryptingMessageStore(
                Buzhou.inMemoryStores().messageStore(), cipher());
        BuzhouMessage original = message("s-1", "findById 内容");
        store.append("s-1", List.of(original));
        assertThat(store.findById(original.id())).contains(original);
        assertThat(store.findById("no-such")).isEmpty();
    }

    @Test
    void legacyPlaintextPassesThrough() {
        MessageStore backing = Buzhou.inMemoryStores().messageStore();
        BuzhouMessage legacy = message("s-1", "迁移前的明文");
        backing.append("s-1", List.of(legacy)); // 直接铺底层（未加密时代）
        EncryptingMessageStore store = new EncryptingMessageStore(backing, cipher());

        assertThat(store.load("s-1")).containsExactly(legacy); // 旧数据原样可读
        assertThat(store.findById(legacy.id())).contains(legacy);
    }

    @Test
    void alreadyEnvelopeNotRewrapped_idempotent() {
        MessageStore backing = Buzhou.inMemoryStores().messageStore();
        EncryptingMessageStore store = new EncryptingMessageStore(backing, cipher());
        BuzhouMessage original = message("s-1", "x");
        store.append("s-1", List.of(original));
        BuzhouMessage carrier = backing.load("s-1").get(0);
        store.append("s-1", List.of(carrier)); // 载体再进——不再包装
        assertThat(backing.load("s-1")).hasSize(2);
        assertThat(store.load("s-1").get(1)).isEqualTo(original); // 往返还原等价
    }

    @Test
    void deleteSessionPassesThrough() {
        MessageStore backing = Buzhou.inMemoryStores().messageStore();
        EncryptingMessageStore store = new EncryptingMessageStore(backing, cipher());
        store.append("s-1", List.of(message("s-1", "x")));
        store.deleteSession("s-1");
        assertThat(backing.load("s-1")).isEmpty();
        assertThat(store.load("s-1")).isEmpty();
    }

    @Test
    void crossSessionCiphertextReplayFails() {
        MessageStore backing = Buzhou.inMemoryStores().messageStore();
        EncryptingMessageStore store = new EncryptingMessageStore(backing, cipher());
        BuzhouMessage original = message("s-1", "绝密");
        store.append("s-1", List.of(original));
        BuzhouMessage carrier = backing.load("s-1").get(0);
        // 剪贴到另一会话（换 sessionId 但同 id——错位投递）
        BuzhouMessage replayed = new BuzhouMessage(carrier.id(), "s-other", carrier.turnSeq(),
                carrier.seqInTurn(), carrier.role(), carrier.content(), carrier.toolCalls(),
                carrier.toolCallId(), carrier.reasoningContent(), carrier.reasoningSignature(),
                carrier.metadata(), carrier.createdAt());
        backing.append("s-other", List.of(replayed));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> store.load("s-other")); // AAD 绑定——错位解密失败宁可炸
    }
}
