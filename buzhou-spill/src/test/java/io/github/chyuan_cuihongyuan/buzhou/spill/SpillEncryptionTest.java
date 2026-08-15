package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 40 §A / T151 / impl-122：spill 落盘静态加密——只经 DiskSpillStore 公开面断言外部行为。
 */
class SpillEncryptionTest {

    @TempDir
    Path rootDir;

    private static String randomKey() {
        byte[] key = new byte[32];
        ThreadLocalRandom.current().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    @Test
    void encryptedAtRestAndTransparentOnLoad() throws Exception {
        SpillCipher cipher = SpillCipher.fromBase64Key(randomKey());
        DiskSpillStore store = new DiskSpillStore(rootDir, SpillQuota.unbounded(), cipher);
        SpillUri uri = new SpillUri("agent", "s1", "tc-enc");
        String secret = "机密工具返回 secret-payload-" + "x".repeat(3000);
        store.store(SpillEntry.of(uri, secret), 2048);

        // 磁盘上只有密文：不含明文、以魔法行开头；meta 仍明文（含 sha256 锚点）
        Path dataFile = rootDir.resolve("agent").resolve("s1").resolve("tc-enc.spill");
        String onDisk = Files.readString(dataFile);
        assertThat(onDisk).startsWith(SpillCipher.MAGIC + "\n");
        assertThat(onDisk).doesNotContain("secret-payload");

        // 读回透明：load 原文 / readRange 正常分页 / 完整性复验通过
        assertThat(store.load(uri)).contains(secret);
        RangeReadResult page = store.readRange(uri, RangeReadRequest.bytes(0, 16));
        assertThat(page.content()).isEqualTo(secret.substring(0, 16));
        assertThat(store.verifyIntegrity(uri)).isTrue();
    }

    @Test
    void freshIvPerWrite() {
        SpillCipher cipher = SpillCipher.fromBase64Key(randomKey());
        String a = cipher.encrypt("same plaintext");
        String b = cipher.encrypt("same plaintext");
        assertThat(a).isNotEqualTo(b); // 随机 IV：同明文两次加密密文不同
        assertThat(cipher.decryptIfEncrypted(a)).isEqualTo("same plaintext");
    }

    @Test
    void legacyPlaintextFilesRemainReadableAfterEnablingEncryption() throws Exception {
        // 先不加密写入（模拟存量明文文件）
        SpillUri uri = new SpillUri("agent", "s1", "tc-legacy");
        new DiskSpillStore(rootDir).store(SpillEntry.of(uri, "legacy plaintext"), 2048);

        // 开启加密后的 store 仍能读旧明文（魔法探测直通）
        DiskSpillStore encrypted = new DiskSpillStore(rootDir, SpillQuota.unbounded(),
                SpillCipher.fromBase64Key(randomKey()));
        assertThat(encrypted.load(uri)).contains("legacy plaintext");
        assertThat(encrypted.readRange(uri, RangeReadRequest.bytes(0, 7)).content())
                .isEqualTo("legacy ");
    }

    @Test
    void wrongKeyFailsFastWithClearSemantics() {
        DiskSpillStore writer = new DiskSpillStore(rootDir, SpillQuota.unbounded(),
                SpillCipher.fromBase64Key(randomKey()));
        SpillUri uri = new SpillUri("agent", "s1", "tc-key");
        writer.store(SpillEntry.of(uri, "data"), 2048);

        DiskSpillStore reader = new DiskSpillStore(rootDir, SpillQuota.unbounded(),
                SpillCipher.fromBase64Key(randomKey()));
        assertThatThrownBy(() -> reader.load(uri))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("encryption-key");
    }

    @Test
    void encryptionOffByDefaultLeavesPlaintextOnDisk() throws Exception {
        DiskSpillStore store = new DiskSpillStore(rootDir);
        SpillUri uri = new SpillUri("agent", "s1", "tc-plain");
        store.store(SpillEntry.of(uri, "plaintext by default"), 2048);

        Path dataFile = rootDir.resolve("agent").resolve("s1").resolve("tc-plain.spill");
        assertThat(Files.readString(dataFile)).isEqualTo("plaintext by default");
        assertThat(store.load(uri)).contains("plaintext by default");
    }

    @Test
    void invalidKeyRejectedAtConstruction() {
        assertThatThrownBy(() -> SpillCipher.fromBase64Key(Base64.getEncoder()
                .encodeToString(new byte[16]))) // 16 字节 = AES-128，不接受
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 字节");
        assertThatThrownBy(() -> SpillCipher.fromBase64Key("not-base64!!"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SpillCipher.fromBase64Key(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
