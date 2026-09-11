package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 511 / T771–T772：归档完整性校验——写时 checksum 随条目落盘、verify
 * OK/MISMATCH/NO_CHECKSUM（存量）/CORRUPT/NO_ARCHIVE、restore 与 purge
 * 级联清理 checksum、verifyAll 清单序。
 */
class SessionArchiverIntegrityTest {

    private static void appendOne(BuzhouStores stores, String sessionId) {
        stores.messageStore().append(sessionId, List.of(new BuzhouMessage(
                UUID.randomUUID().toString(), sessionId, 1, 0, Role.USER,
                "x " + sessionId, List.of(), null, null, null, Map.of(), Instant.now())));
    }

    @Test
    void archiveThenVerifyIsOkAndRestoreCleansChecksum() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        SessionArchiver archiver = new SessionArchiver(stores, new SessionCleaner(stores));
        appendOne(stores, "sess-ok");
        assertThat(archiver.archive("sess-ok")).isTrue();
        assertThat(archiver.verify("sess-ok").state())
                .isEqualTo(SessionArchiver.VerifyState.OK);

        // restore 级联清 checksum——恢复后无归档
        archiver.restore("sess-ok");
        assertThat(archiver.verify("sess-ok").state())
                .isEqualTo(SessionArchiver.VerifyState.NO_ARCHIVE);
    }

    @Test
    void tamperedColdLayerFailsVerification() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        SessionArchiver archiver = new SessionArchiver(stores, new SessionCleaner(stores));
        appendOne(stores, "sess-tamper");
        archiver.archive("sess-tamper");

        // 模拟冷层被改（直接改写归档值——校验和不变 → MISMATCH）
        var stateStore = stores.sessionStateStore();
        String key = SessionArchiver.ARCHIVE_PREFIX + "sess-tamper";
        var entry = stateStore.get(SessionArchiver.ARCHIVE_SESSION_ID, key).orElseThrow();
        String tampered = entry.value().replace("x sess-tamper", "tampered");
        stateStore.put(SessionArchiver.ARCHIVE_SESSION_ID, new StateEntry(
                key, tampered, "attacker", 0, null, Instant.now()));

        var result = archiver.verify("sess-tamper");
        assertThat(result.state()).isEqualTo(SessionArchiver.VerifyState.CHECKSUM_MISMATCH);
        assertThat(result.detail()).isNotBlank(); // 记录在案的原校验和可见
        assertThat(archiver.verifyAll())
                .anySatisfy(r -> assertThat(r.state())
                        .isEqualTo(SessionArchiver.VerifyState.CHECKSUM_MISMATCH));
    }

    @Test
    void legacyArchiveWithoutChecksumIsDistinguished() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        SessionArchiver archiver = new SessionArchiver(stores, new SessionCleaner(stores));
        appendOne(stores, "sess-legacy");
        archiver.archive("sess-legacy");
        // 抹掉 checksum 键模拟本特性之前的存量归档
        stores.sessionStateStore().delete(SessionArchiver.ARCHIVE_CHECKSUM_SESSION_ID,
                SessionArchiver.ARCHIVE_CHECKSUM_PREFIX + "sess-legacy");
        assertThat(archiver.verify("sess-legacy").state())
                .isEqualTo(SessionArchiver.VerifyState.NO_CHECKSUM);
    }

    @Test
    void corruptArchiveJsonReported() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        SessionArchiver archiver = new SessionArchiver(stores, new SessionCleaner(stores));
        appendOne(stores, "sess-corrupt");
        archiver.archive("sess-corrupt");
        var stateStore = stores.sessionStateStore();
        String key = SessionArchiver.ARCHIVE_PREFIX + "sess-corrupt";
        stateStore.put(SessionArchiver.ARCHIVE_SESSION_ID, new StateEntry(
                key, "{not-json", "attacker", 0, null, Instant.now()));
        assertThat(archiver.verify("sess-corrupt").state())
                .isEqualTo(SessionArchiver.VerifyState.CORRUPT);
    }

    @Test
    void verifyAllCoversEveryArchiveInOrder() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        SessionArchiver archiver = new SessionArchiver(stores, new SessionCleaner(stores));
        appendOne(stores, "b");
        archiver.archive("b");
        appendOne(stores, "a");
        archiver.archive("a");
        List<SessionArchiver.VerifyResult> results = archiver.verifyAll();
        assertThat(results).hasSize(2);
        assertThat(results).allSatisfy(r -> assertThat(r.state())
                .isEqualTo(SessionArchiver.VerifyState.OK));
        assertThat(results.get(0).sessionId()).isEqualTo("a"); // 字典序
    }
}
