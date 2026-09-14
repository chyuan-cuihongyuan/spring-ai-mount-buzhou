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
 * spec 1097 / impl 849：offload×cipher 加密联动组合——加密开启的 SpillStore 上
 * offload 必伴随 encrypt 调用（加密配置真实生效的组合验证）。纯测试轮。
 */
class OffloadCipherComboTest {

    @TempDir
    Path rootDir;

    @BeforeEach
    void reset() {
        SpillOffloadHook.resetForTest();
        SpillCipher.resetForTest();
    }

    private SpillCipher randomCipher() {
        byte[] key = new byte[32];
        new java.security.SecureRandom().nextBytes(key);
        return SpillCipher.fromBase64Key(Base64.getEncoder().encodeToString(key));
    }

    private SpillOffloadHook hook(SpillService service) {
        SessionReadOnlyRegistry registry = new SessionReadOnlyRegistry();
        DiskSpillStore store = new DiskSpillStore(rootDir, null, null);
        return new SpillOffloadHook(service, registry,
                uri -> store.dataPathOf(uri), 100, Map.of());
    }

    @Test
    void offloadOnEncryptedStoreTriggersEncrypt() {
        SpillCipher cipher = randomCipher();
        DiskSpillStore encryptedStore = new DiskSpillStore(rootDir, null, cipher);
        SpillService service = new SpillService(encryptedStore, 64, 3);
        SpillOffloadHook offload = hook(service);

        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
        DefaultToolCallContext ctx = new DefaultToolCallContext(env, "tc1", "big_tool", Map.of());
        ctx.markExecuted("x".repeat(600), null);
        offload.afterTool(ctx);

        SpillOffloadHook.SpillOffloadStats os = SpillOffloadHook.stats();
        assertThat(os.offloaded()).isEqualTo(1);
        // 组合一致性：offload 发生则加密必然发生（加密配置真实生效）
        assertThat(SpillCipher.stats().encryptCalls()).isGreaterThanOrEqualTo(1);

        // 守恒保持
        assertThat(os.invocations()).isEqualTo(os.durableSkips() + os.errorSkips()
                + os.cleanInline() + os.offloaded() + os.refrains());
    }

    @Test
    void resetsAreIndependent() {
        SpillCipher cipher = randomCipher();
        DiskSpillStore encryptedStore = new DiskSpillStore(rootDir, null, cipher);
        SpillService service = new SpillService(encryptedStore, 64, 3);
        SpillOffloadHook offload = hook(service);

        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
        DefaultToolCallContext ctx = new DefaultToolCallContext(env, "tc1", "big_tool", Map.of());
        ctx.markExecuted("x".repeat(600), null);
        offload.afterTool(ctx);

        SpillOffloadHook.resetForTest();
        assertThat(SpillOffloadHook.stats().invocations()).isZero();
        assertThat(SpillCipher.stats().encryptCalls()).isGreaterThanOrEqualTo(1);

        SpillCipher.resetForTest();
        assertThat(SpillCipher.stats().encryptCalls()).isZero();
    }
}
