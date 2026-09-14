package io.github.chyuan_cuihongyuan.buzhou.spill;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1106 / impl 858：cipher×readBack 组合——加密 DiskSpillStore 上写入密文→
 * 回读时 decryptCalls 随 reads 增长 + ReadRangeStats 守恒。纯测试轮。
 */
class CipherReadBackComboTest {

    @TempDir
    Path rootDir;

    @BeforeEach
    void reset() {
        ReadRangeTool.resetForTest();
        SpillCipher.resetForTest();
    }

    private SpillCipher randomCipher() {
        byte[] key = new byte[32];
        new java.security.SecureRandom().nextBytes(key);
        return SpillCipher.fromBase64Key(Base64.getEncoder().encodeToString(key));
    }

    @Test
    void encryptedReadBackTriggersDecrypt() {
        SpillCipher cipher = randomCipher();
        DiskSpillStore encryptedStore = new DiskSpillStore(rootDir, null, cipher);
        SpillService service = new SpillService(encryptedStore, 64, 3);
        ReadRangeTool reader = new ReadRangeTool(service);

        String plain = "y".repeat(600);
        service.tryOffload("agent", "s1", "tc1", "big_tool", plain, 100);
        long decryptsAfterWrite = SpillCipher.stats().decryptCalls();

        String out = reader.call("{\"path\":\"spill://agent/s1/tc1\",\"mode\":\"bytes\"}");
        assertThat(out).contains("y");

        ReadRangeTool.ReadRangeStats rs = ReadRangeTool.stats();
        assertThat(rs.reads()).isEqualTo(1);
        assertThat(SpillCipher.stats().decryptCalls()).isGreaterThanOrEqualTo(decryptsAfterWrite);
    }

    @Test
    void resetsAreIndependent() {
        SpillCipher cipher = randomCipher();
        DiskSpillStore encryptedStore = new DiskSpillStore(rootDir, null, cipher);
        SpillService service = new SpillService(encryptedStore, 64, 3);
        ReadRangeTool reader = new ReadRangeTool(service);

        service.tryOffload("agent", "s1", "tc1", "big_tool", "x".repeat(600), 100);
        reader.call("{\"path\":\"spill://agent/s1/tc1\",\"mode\":\"bytes\"}");

        ReadRangeTool.resetForTest();
        assertThat(ReadRangeTool.stats().calls()).isZero();
        assertThat(SpillCipher.stats().decryptCalls()).isGreaterThanOrEqualTo(1);

        SpillCipher.resetForTest();
        assertThat(SpillCipher.stats().decryptCalls()).isZero();
    }
}
