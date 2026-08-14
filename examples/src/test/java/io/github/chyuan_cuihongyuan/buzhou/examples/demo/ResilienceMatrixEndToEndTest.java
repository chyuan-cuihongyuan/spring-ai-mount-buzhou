package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.guard.audit.AgentAuditRecord;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.AuditChain;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.AuditChainVerifier;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.AuditRecordStore;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.AuditTrailCollector;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.InMemoryAuditRecordStore;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.SigningKeyProvider;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.SigningKeyRing;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.VerificationReport;
import io.github.chyuan_cuihongyuan.buzhou.guard.policy.PolicyDecision;
import io.github.chyuan_cuihongyuan.buzhou.guard.policy.PolicyRefresher;
import io.github.chyuan_cuihongyuan.buzhou.guard.policy.ResourcePolicySource;
import io.github.chyuan_cuihongyuan.buzhou.guard.sandbox.CommandSandbox;
import io.github.chyuan_cuihongyuan.buzhou.guard.sandbox.DenoSandbox;
import io.github.chyuan_cuihongyuan.buzhou.guard.sandbox.LimitedCommandSandbox;
import io.github.chyuan_cuihongyuan.buzhou.guard.sandbox.SandboxLimits;
import io.github.chyuan_cuihongyuan.buzhou.guard.sandbox.SandboxProcessLauncher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-43 / spec 13 §T69 韧性回归矩阵统一入口：spec 13 §13 列举场景中未随切片 39-42
 * 落到 examples 的跨片场景在此统一回归——
 *
 * <ul>
 *   <li>审计链篡改 → 独立校验定位首个断点（39）；</li>
 *   <li>密钥轮换 → 旧记录仍可验、新记录用新钥（39）；</li>
 *   <li>policy 热更 → 新决策用新规则 + provenance 可溯源（40）；</li>
 *   <li>沙箱输出超限 → 截断显式标记（40）；</li>
 *   <li>审计持久化 → 跨重启（store 重开）链完整（39）。</li>
 * </ul>
 *
 * <p>已随片落地的场景（挂起→deadline、慢监听→背压、续租 steal→LeaseLost、停机排空、
 * 写失败双策略、脏 JSON、熔断半开、MySQL 幂等/基线迁移）见各自 demo 测试与 store-jdbc
 * Testcontainers 套件——本入口只补跨片缺口，不重复回归。
 */
class ResilienceMatrixEndToEndTest {

    @TempDir
    Path tempDir;

    private static SigningKeyRing ringWith(KeyPair pair) {
        return new SigningKeyRing(0, List.of(new SigningKeyProvider.VersionedSigningKey(
                1, pair.getPrivate(), pair.getPublic())));
    }

    @Test
    void auditTamperIsLocatedByIndependentReplay() {
        SigningKeyRing ring = ringWith(AuditChain.generateKeyPair());
        AuditChain chain = new AuditChain("matrix", "1", ring);
        chain.append("s1", "guard.tool.blocked", "{}", "BLOCKED");
        chain.append("s1", "guard.auth.granted", "{}", "ALLOWED");
        chain.append("s1", "session.cancelled", "{}", "RECORDED");

        // 篡改第 2 条内容（保留签名）→ 重放定位首个断点为该记录自身（签名失配）
        AgentAuditRecord second = chain.records().get(1);
        List<AgentAuditRecord> tampered = new ArrayList<>(chain.records());
        tampered.set(1, new AgentAuditRecord(second.recordId(), second.timestamp(),
                second.agentId(), second.agentVersion(), second.sessionId(),
                second.actionType(), "{\"evil\":true}", second.outcome(),
                second.trustLevel(), second.parentRecordId(), second.prevHash(),
                second.signature(), second.keyVersion()));

        VerificationReport report = AuditChainVerifier.verify(tampered, ring);
        assertThat(report.intact()).isFalse();
        assertThat(report.firstBreakIndex()).isEqualTo(1);
        assertThat(report.brokenRecordId()).isEqualTo(second.recordId());
        // 未篡改链全绿
        assertThat(AuditChainVerifier.verify(chain.records(), ring).intact()).isTrue();
    }

    @Test
    void keyRotationKeepsOldRecordsVerifiableAndSignsWithNewKey() {
        KeyPair v1 = AuditChain.generateKeyPair();
        SigningKeyRing ring = ringWith(v1);
        AuditChain chain = new AuditChain("matrix", "1", ring);
        AgentAuditRecord before = chain.append("s1", "guard.tool.blocked", "{}", "BLOCKED");

        KeyPair v2 = AuditChain.generateKeyPair();
        ring.rotate(2, v2);
        AgentAuditRecord after = chain.append("s1", "guard.auth.granted", "{}", "ALLOWED");

        assertThat(before.keyVersion()).isEqualTo(1);
        assertThat(after.keyVersion()).isEqualTo(2);
        // 旧记录仍可验（旧公钥保留在环上，单钥重放全绿）；新记录用旧公钥验不过（只验不签）
        assertThat(AuditChainVerifier.verify(List.of(before), ringWith(v1)).intact()).isTrue();
        assertThat(AuditChainVerifier.verify(List.of(after), ringWith(v1)).intact()).isFalse();
        assertThat(chain.verify(ring)).isTrue();
    }

    @Test
    void policyHotReloadTakesEffectWithProvenance() throws Exception {
        Path file = Files.createTempFile(tempDir, "matrix-policy", ".json");
        Files.writeString(file, """
                {"rules":[{"id":"deny-all","toolPattern":"*","action":"DENY","reason":"v1"}]}""",
                StandardCharsets.UTF_8);
        try (PolicyRefresher refresher = new PolicyRefresher(
                new ResourcePolicySource("file:" + file), Duration.ZERO)) {
            PolicyDecision.Input input = new PolicyDecision.Input("matrix", "read_file",
                    Map.of(), Map.of("taint", "TRUSTED"), false);
            assertThat(refresher.decide(input).action()).isEqualTo(PolicyDecision.Action.DENY);

            Files.writeString(file, """
                    {"rules":[{"id":"allow-trusted","toolPattern":"read_*",
                     "labels":[{"key":"taint","op":"eq","value":"TRUSTED"}],
                     "action":"ALLOW","reason":"v2"}]}""", StandardCharsets.UTF_8);
            refresher.refresh();

            PolicyDecision after = refresher.decide(input);
            assertThat(after.action()).isEqualTo(PolicyDecision.Action.ALLOW);
            assertThat(after.reason()).contains("allow-trusted");
            assertThat(after.revision()).isEqualTo(refresher.revision());
        }
    }

    @Test
    void sandboxOversizedOutputIsExplicitlyTruncated() {
        List<Duration> timeouts = new CopyOnWriteArrayList<>();
        SandboxProcessLauncher launcher = new SandboxProcessLauncher() {
            @Override
            public CommandSandbox.CommandResult launch(List<String> argv,
                    Map<String, String> env, Path workDir, Duration timeout) {
                timeouts.add(timeout);
                if (argv.size() == 2 && argv.get(1).equals("--version")) {
                    return new CommandSandbox.CommandResult(0, "deno", "", false);
                }
                return new CommandSandbox.CommandResult(0, "x".repeat(1000), "", false);
            }
        };
        LimitedCommandSandbox sandbox = new LimitedCommandSandbox(
                DenoSandbox.builder(launcher).build(),
                new SandboxLimits(Duration.ofSeconds(5), 100L, null));

        CommandSandbox.CommandResult result = sandbox.run(List.of("cat", "big"), Map.of(),
                null, Duration.ofSeconds(60));

        assertThat(timeouts.getLast()).isEqualTo(Duration.ofSeconds(5)); // 超时取更小者
        assertThat(result.truncated()).isTrue();
        assertThat(result.killedReason())
                .isEqualTo(CommandSandbox.CommandResult.KilledReason.OUTPUT);
        assertThat(result.stdout()).hasSize(100);
    }

    @Test
    void auditChainSurvivesStoreRestart() {
        AuditRecordStore store = new InMemoryAuditRecordStore(64);
        SigningKeyRing ring = ringWith(AuditChain.generateKeyPair());
        AuditChain first = new AuditChain("matrix", "1", ring);
        first.append("s1", "guard.tool.blocked", "{}", "BLOCKED");
        first.records().forEach(store::append);

        // 「重启」：同一密钥环（PEM 文件重建）+ 从持久化续链再追加新记录
        AuditChain second = new AuditChain("matrix", "1", ring);
        second.resume(store.loadAll());
        AgentAuditRecord appended = second.append("s1", "session.cancelled", "{}", "RECORDED");
        store.append(appended);

        // 独立校验器全量重放：跨重启哈希链与签名都连续
        VerificationReport report = AuditChainVerifier.verify(store.loadAll(), ring);
        assertThat(report.intact()).isTrue();
        assertThat(report.verifiedCount()).isEqualTo(2);
    }

    /** 收集器冒烟：guard 事件进审计链、session.closed 发布收尾摘要、无签名降级仍可验。 */
    @Test
    void collectorFeedsAuditChainWithGuardEvents() {
        AuditChain chain = new AuditChain("matrix", "1"); // 无密钥 → 纯哈希链降级
        AuditTrailCollector collector = new AuditTrailCollector(chain, null);
        collector.onEvent(io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent.of(
                "guard.tool.blocked", Map.of("sessionId", "s1")));
        collector.onEvent(
                io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent.of("session.closed"));
        assertThat(chain.records()).hasSize(2);
        assertThat(chain.records().getLast().actionType()).isEqualTo("audit.session.closed");
        assertThat(chain.verify((java.security.PublicKey) null)).isTrue();
    }
}
