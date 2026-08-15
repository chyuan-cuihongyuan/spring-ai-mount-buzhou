package io.github.chyuan_cuihongyuan.buzhou.examples.golden;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.EventSequenceAssert;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.FakeChatModel;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptStep;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.AuditChain;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.AuditChainVerifier;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.PemFileKeyPersister;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.PemFileKeyProvider;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.SigningKeyRing;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.VerificationReport;
import io.github.chyuan_cuihongyuan.buzhou.spill.DiskSpillStore;
import io.github.chyuan_cuihongyuan.buzhou.spill.RangeReadRequest;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillCipher;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillEntry;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillModule;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillQuota;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillUri;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 黄金轨迹 E（spec 45 §A / T161 / impl-132）：effort #9 新机制——会话单飞闸、
 * 审计密钥轮换持久化（重启入环）、spill 加密往返（含旧明文兼容）。
 */
class GoldenTrajectoryEffort9Test {

    // ---- G19 单飞闸：在途拒绝 → 终结释放 → 续轮正常 ----

    @Test
    void g19SingleFlightGateTrajectory() throws Exception {
        CountDownLatch toolEntered = new CountDownLatch(1);
        CountDownLatch releaseTool = new CountDownLatch(1);
        // 首轮工具挂住（在途）；并发第二轮必须确定拒绝；放行后首轮正常收尾
        FakeChatModel model = FakeChatModel.script(
                ScriptStep.toolCall("hang_tool", "{}"),
                ScriptStep.text("首轮完成"));
        org.springframework.ai.tool.ToolCallback hangingTool = new org.springframework.ai.tool.ToolCallback() {
            @Override
            public org.springframework.ai.tool.definition.ToolDefinition getToolDefinition() {
                return org.springframework.ai.tool.definition.ToolDefinition.builder()
                        .name("hang_tool").description("hangs until released").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                toolEntered.countDown();
                try {
                    releaseTool.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "tool-done";
            }
        };
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(), hangingTool);
        io.github.chyuan_cuihongyuan.buzhou.core.internal.session.DefaultAgentSession session =
                (io.github.chyuan_cuihongyuan.buzhou.core.internal.session.DefaultAgentSession)
                        runtime.spawn("app", "ag", "g19");

        CompletableFuture<String> first =
                CompletableFuture.supplyAsync(() -> session.chat("第一轮"));
        assertThat(toolEntered.await(5, TimeUnit.SECONDS)).isTrue();

        // 在途期间的并发入口：确定拒绝（TURN_IN_FLIGHT），而非未定义并发
        assertThatThrownBy(() -> session.chat("并发"))
                .isInstanceOf(BuzhouException.class)
                .satisfies(e -> assertThat(((BuzhouException) e).errorCode())
                        .isEqualTo(ErrorCode.TURN_IN_FLIGHT));

        releaseTool.countDown();
        assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo("首轮完成");
        // 闸收口：在途归零、后续轮次正常（轨迹终点可续）
        assertThat(session.inFlightTurns()).isZero();
        FakeChatModel followUp = FakeChatModel.script(ScriptStep.text("第二轮完成"));
        io.github.chyuan_cuihongyuan.buzhou.core.internal.session.DefaultAgentRuntime runtime2 =
                new io.github.chyuan_cuihongyuan.buzhou.core.internal.session.DefaultAgentRuntime(
                        followUp, Buzhou.inMemoryStores(),
                        new io.github.chyuan_cuihongyuan.buzhou.core.internal.session.HarnessAssembler(),
                        io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults(),
                        null, null, Duration.ofSeconds(30));
        var session2 = runtime2.spawn("app", "ag", "g19b");
        assertThat(session2.chat("第二轮")).isEqualTo("第二轮完成");
        runtime2.close();
    }

    // ---- G20 审计轮换持久化：v1 落链 → rotate 落盘 → v2 落链 → 重启扫描仍全链可验 ----

    @Test
    void g20AuditRotationPersistTrajectory(@TempDir Path keyDir) {
        // v1 以 persister 落盘（与运维初始部署同路径），扫描入环
        new PemFileKeyPersister(keyDir).persist(1, AuditChain.generateKeyPair());
        SigningKeyRing ring = new SigningKeyRing(0, PemFileKeyProvider.scanDirectory(keyDir).load(),
                new PemFileKeyPersister(keyDir));

        AuditChain chain = new AuditChain("g20", "1", ring);
        chain.append("s1", "guard.tool.blocked", "{}", "BLOCKED"); // v1 签名
        assertThat(chain.records().getFirst().keyVersion()).isEqualTo(1);

        KeyPairWrap v2 = KeyPairWrap.generate();
        ring.rotate(2, v2.pair); // 写而后切：v2.pem 已落盘
        chain.append("s1", "guard.auth.granted", "{}", "ALLOWED"); // v2 签名
        assertThat(chain.records().get(1).keyVersion()).isEqualTo(2);

        // 「重启」：目录扫描重建环——两版本都可验、active=v2（轮换不因重启断链）
        SigningKeyRing restarted = new SigningKeyRing(0,
                PemFileKeyProvider.scanDirectory(keyDir).load(), null);
        VerificationReport report = AuditChainVerifier.verify(chain.records(), restarted);
        assertThat(report.intact()).isTrue();
        assertThat(report.keyVersionStats()).containsEntry("1", 1L).containsEntry("2", 1L);
        // 外锚：完整链头锚定通过、删尾可检
        String anchor = report.headHash();
        assertThat(AuditChainVerifier.verify(chain.records(), restarted, anchor).anchored()).isTrue();
        assertThat(AuditChainVerifier.verify(
                chain.records().subList(0, 1), restarted, anchor).anchored()).isFalse();
    }

    private record KeyPairWrap(java.security.KeyPair pair) {
        static KeyPairWrap generate() {
            return new KeyPairWrap(AuditChain.generateKeyPair());
        }
    }

    // ---- G21 spill 加密往返：密文落盘 → 透明读回 → 旧明文兼容 ----

    @Test
    void g21SpillEncryptionRoundTrip(@TempDir Path rootDir) throws Exception {
        byte[] key = new byte[32];
        ThreadLocalRandom.current().nextBytes(key);
        SpillCipher cipher = SpillCipher.fromBase64Key(Base64.getEncoder().encodeToString(key));
        DiskSpillStore store = new DiskSpillStore(rootDir, SpillQuota.unbounded(), cipher);

        SpillUri uri = new SpillUri("agent", "g21", "tc-1");
        String secret = "敏感大结果 " + "x".repeat(4000);
        store.store(SpillEntry.of(uri, secret), 2048);

        // 磁盘密文 + 透明读回 + 完整性锚点有效
        Path dataFile = rootDir.resolve("agent").resolve("g21").resolve("tc-1.spill");
        assertThat(Files.readString(dataFile)).startsWith(SpillCipher.MAGIC).doesNotContain("敏感大结果");
        assertThat(store.load(uri)).contains(secret);
        assertThat(store.readRange(uri, RangeReadRequest.bytes(0, 5)).content()).isEqualTo("敏感大结果".substring(0, 5));
        assertThat(store.verifyIntegrity(uri)).isTrue();

        // 旧明文文件（升级前落盘）在加密 store 下兼容读
        SpillUri legacy = new SpillUri("agent", "g21", "tc-legacy");
        new DiskSpillStore(rootDir).store(SpillEntry.of(legacy, "legacy plaintext"), 2048);
        assertThat(store.load(legacy)).contains("legacy plaintext");

        // SpillModule 带密钥构造（装配面闭环）
        assertThat(new SpillModule(rootDir, 64, 4, SpillQuota.unbounded(), cipher)).isNotNull();
    }
}
