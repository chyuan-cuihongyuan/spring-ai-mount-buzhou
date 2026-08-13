package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-17 / T45 内容寻址完整性：落盘记录 whole sha256、读回复验；
 * 篡改可检测（读侧 lenient=warning 前缀）；envelope chunk 复验闭环。
 */
class ReadIntegrityTest {

    @TempDir
    Path root;

    @Test
    void storedEntryVerifiesAndTamperingIsDetectedAsReadSideWarning() throws Exception {
        DiskSpillStore store = new DiskSpillStore(root);
        SpillUri uri = new SpillUri("agent", "s-" + UUID.randomUUID(), "tc-1");
        String content = "订单数据 ORD-1：状态 DELIVERED\n" + "x".repeat(500);
        store.store(new SpillEntry(uri, content, "text/plain", content.length(), Instant.now()), 64);

        // 完好：复验通过、读回无告警
        assertThat(store.verifyIntegrity(uri)).isTrue();
        assertThat(store.readRange(uri, RangeReadRequest.bytes(0, 20)).content())
                .doesNotContain(ReadIntegrity.CORRUPTION_WARNING);

        // 篡改数据文件（模拟腐化/TOCTOU）：复验失败、读回内容前缀完整性告警（lenient 透传）
        Path dataFile = root.resolve(uri.agentName()).resolve(uri.sessionId())
                .resolve(uri.toolCallId() + ".spill");
        Files.writeString(dataFile, content.replace("DELIVERED", "CANCELLED"));
        assertThat(store.verifyIntegrity(uri)).isFalse();
        String readBack = store.readRange(uri, RangeReadRequest.bytes(0, 40)).content();
        assertThat(readBack).startsWith(ReadIntegrity.CORRUPTION_WARNING);
        assertThat(readBack).contains("CANCELLED"); // 数据仍透传（读侧降级不阻断），但带告警
    }

    @Test
    void legacyMetaWithoutHashVerifiesAsTrue() throws Exception {
        DiskSpillStore store = new DiskSpillStore(root);
        SpillUri uri = new SpillUri("agent", "s-" + UUID.randomUUID(), "tc-2");
        store.store(new SpillEntry(uri, "legacy", "text/plain", 6, Instant.now()), 64);
        // 抹掉 meta 中的摘要字段（模拟旧条目）→ 无法复验按通过（向后兼容）
        Path metaFile = root.resolve(uri.agentName()).resolve(uri.sessionId())
                .resolve(uri.toolCallId() + ".meta");
        Files.writeString(metaFile, "{\"sizeChars\":6,\"contentType\":\"text/plain\"}");
        assertThat(store.verifyIntegrity(uri)).isTrue();
    }

    @Test
    void envelopeChunkVerificationClosesTheLoop() {
        String whole = "abcdefghij".repeat(50);
        String slice = whole.substring(100, 160);
        ReadIntegrity.IntegrityEnvelope envelope =
                ReadIntegrity.envelope(slice, 100, 60, whole);

        assertThat(envelope.byteRange()).isEqualTo("100..160");
        assertThat(ReadIntegrity.chunkVerified(envelope)).isTrue();
        assertThat(envelope.wholeSha256()).isEqualTo(ReadIntegrity.sha256(whole));
        // 数据被篡改后复验失败
        ReadIntegrity.IntegrityEnvelope tampered = new ReadIntegrity.IntegrityEnvelope(
                slice.replace('a', 'z'), envelope.byteRange(), envelope.chunkSha256(),
                envelope.wholeSha256());
        assertThat(ReadIntegrity.chunkVerified(tampered)).isFalse();
    }
}
