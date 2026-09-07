package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SummaryStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 336 / impl-359：摘要槽加密装饰器回归——底层只见载体（结构键+密文）/
 * save-latest-history 全往返（版本以底层为准）/旧明文透传/幂等不重复包装/
 * AAD 换绑失败。
 */
class EncryptingSummaryStoreTest {

    private static EnvelopeCipher cipher() {
        byte[] key = new byte[32];
        java.util.Arrays.fill(key, (byte) 9);
        return new EnvelopeCipher(Base64.getEncoder().encodeToString(key), null);
    }

    private static StructuredSummary summary(String sessionId, long version) {
        return new StructuredSummary(sessionId, version,
                Map.of("P0", "用户张三的长期目标", "P1", "偏好简体中文"),
                123, Instant.parse("2026-09-04T12:00:00Z"));
    }

    @Test
    void underlyingStoreSeesOnlyCarrier_plaintextNeverAtRest() {
        SummaryStore backing = Buzhou.inMemoryStores().summaryStore();
        EncryptingSummaryStore store = new EncryptingSummaryStore(backing, cipher());
        store.save("s-1", summary("s-1", 0));

        StructuredSummary atRest = backing.latest("s-1").orElseThrow();
        assertThat(atRest.sections()).containsOnlyKeys(EncryptingSummaryStore.ENVELOPE_SECTION);
        assertThat(atRest.sections().values().iterator().next())
                .startsWith("buzhou:v1:").doesNotContain("张三");
        assertThat(atRest.tokenEstimate()).isEqualTo(123); // 路由元数据明文
        assertThat(atRest.version()).isEqualTo(1L); // 底层 UPSERT 分配真版本
    }

    @Test
    void latestRestoresAllFields_versionFromUnderlying() {
        EncryptingSummaryStore store = new EncryptingSummaryStore(
                Buzhou.inMemoryStores().summaryStore(), cipher());
        store.save("s-1", summary("s-1", 0));

        Optional<StructuredSummary> latest = store.latest("s-1");
        assertThat(latest).isPresent();
        StructuredSummary restored = latest.get();
        assertThat(restored.version()).isEqualTo(1L); // 载体 0 占位——还原以底层为准
        assertThat(restored.sections()).containsEntry("P0", "用户张三的长期目标");
        assertThat(restored.tokenEstimate()).isEqualTo(123);
        assertThat(restored.createdAt()).isEqualTo(Instant.parse("2026-09-04T12:00:00Z"));
    }

    @Test
    void historyRestoresEveryVersion() {
        EncryptingSummaryStore store = new EncryptingSummaryStore(
                Buzhou.inMemoryStores().summaryStore(), cipher());
        store.save("s-1", summary("s-1", 0));
        store.save("s-1", new StructuredSummary("s-1", 0,
                Map.of("P0", "第二版"), 50, Instant.parse("2026-09-04T13:00:00Z")));

        assertThat(store.history("s-1", 10)).hasSize(2);
        assertThat(store.latest("s-1").orElseThrow().sections())
                .containsEntry("P0", "第二版");
    }

    @Test
    void legacyPlaintextPassesThrough() {
        SummaryStore backing = Buzhou.inMemoryStores().summaryStore();
        StructuredSummary legacy = summary("s-1", 1);
        backing.save("s-1", legacy); // 直接铺底层（未加密时代）
        EncryptingSummaryStore store = new EncryptingSummaryStore(backing, cipher());

        assertThat(store.latest("s-1")).contains(legacy); // 旧数据原样可读
    }

    @Test
    void carrierReSaveNotRewrapped_idempotent() {
        SummaryStore backing = Buzhou.inMemoryStores().summaryStore();
        EncryptingSummaryStore store = new EncryptingSummaryStore(backing, cipher());
        store.save("s-1", summary("s-1", 0));
        StructuredSummary carrier = backing.latest("s-1").orElseThrow();
        long version = store.save("s-1", carrier); // 载体再存——不再包装
        assertThat(backing.latest("s-1").orElseThrow().sections())
                .containsOnlyKeys(EncryptingSummaryStore.ENVELOPE_SECTION);
        assertThat(version).isEqualTo(2L);
    }

    @Test
    void tamperedRoutingFieldFailsDecryption() {
        SummaryStore backing = Buzhou.inMemoryStores().summaryStore();
        EncryptingSummaryStore store = new EncryptingSummaryStore(backing, cipher());
        store.save("s-1", summary("s-1", 0));
        // 篡改载体 createdAt（AAD 绑定）——换绑后解密必炸
        StructuredSummary carrier = backing.latest("s-1").orElseThrow();
        StructuredSummary tampered = new StructuredSummary(carrier.sessionId(),
                carrier.version(), carrier.sections(), carrier.tokenEstimate(),
                Instant.parse("2026-09-05T00:00:00Z"));
        backing.save("s-2", tampered);
        assertThatThrownBy(() -> new EncryptingSummaryStore(backing, cipher()).latest("s-2"))
                .isInstanceOf(IllegalStateException.class); // 完整性优先
    }
}
