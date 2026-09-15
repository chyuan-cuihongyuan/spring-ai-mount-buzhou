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
 * spec 1213 / impl 877：加密溢出×逐出组合——加密 DiskSpillStore 上溢出后
 * evict 句柄，SpillOffloadStats 与 EvictHandleStats 各自计数一致 + 守恒。纯测试轮。
 */
class CipherEvictComboTest {

    @TempDir
    Path rootDir;

    @BeforeEach
    void reset() {
        SpillOffloadHook.resetForTest();
        EvictHandleTool.resetForTest();
        SpillCipher.resetForTest();
    }

    private SpillCipher randomCipher() {
        byte[] key = new byte[32];
        new java.security.SecureRandom().nextBytes(key);
        return SpillCipher.fromBase64Key(Base64.getEncoder().encodeToString(key));
    }

    @Test
    void encryptedOffloadThenEvictCountsBothSides() {
        SpillCipher cipher = randomCipher();
        DiskSpillStore encryptedStore = new DiskSpillStore(rootDir, null, cipher);
        SpillService service = new SpillService(encryptedStore, 64, 3);
        SessionReadOnlyRegistry registry = new SessionReadOnlyRegistry();
        SpillOffloadHook offload = new SpillOffloadHook(service, registry,
                uri -> ((DiskSpillStore) encryptedStore).dataPathOf(uri), 100, Map.of());
        EvictHandleTool evict = new EvictHandleTool(new HandleLifecycleRegistry());

        // 溢出（加密 store）
        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
        DefaultToolCallContext ctx = new DefaultToolCallContext(env, "tc1", "big_tool", Map.of());
        ctx.markExecuted("x".repeat(600), null);
        offload.afterTool(ctx);
        assertThat(SpillOffloadHook.stats().offloaded()).isEqualTo(1);

        // 逐出该句柄
        evict.call("{\"path\":\"spill://agent/s1/tc1\"}");
        assertThat(EvictHandleTool.stats().evictions()).isEqualTo(1);

        // 双读面各自守恒
        SpillOffloadHook.SpillOffloadStats os = SpillOffloadHook.stats();
        assertThat(os.invocations()).isEqualTo(os.durableSkips() + os.errorSkips()
                + os.cleanInline() + os.offloaded() + os.refrains());
        EvictHandleTool.EvictStats es = EvictHandleTool.stats();
        assertThat(es.attempts()).isEqualTo(es.evictions()
                + es.badPathRejects() + es.parseRejects());
    }

    @Test
    void resetsAreIndependent() {
        SpillCipher cipher = randomCipher();
        DiskSpillStore encryptedStore = new DiskSpillStore(rootDir, null, cipher);
        SpillService service = new SpillService(encryptedStore, 64, 3);
        SessionReadOnlyRegistry registry = new SessionReadOnlyRegistry();
        SpillOffloadHook offload = new SpillOffloadHook(service, registry,
                uri -> encryptedStore.dataPathOf(uri), 100, Map.of());
        EvictHandleTool evict = new EvictHandleTool(new HandleLifecycleRegistry());

        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
        DefaultToolCallContext ctx = new DefaultToolCallContext(env, "tc1", "big_tool", Map.of());
        ctx.markExecuted("x".repeat(600), null);
        offload.afterTool(ctx);
        evict.call("{\"path\":\"spill://agent/s1/tc1\"}");

        SpillOffloadHook.resetForTest();
        assertThat(SpillOffloadHook.stats().offloaded()).isZero();
        assertThat(EvictHandleTool.stats().evictions()).isEqualTo(1);

        EvictHandleTool.resetForTest();
        assertThat(EvictHandleTool.stats().evictions()).isZero();
    }
}
