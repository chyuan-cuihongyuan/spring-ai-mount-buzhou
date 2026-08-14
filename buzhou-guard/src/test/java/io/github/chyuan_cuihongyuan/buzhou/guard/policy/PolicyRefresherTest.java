package io.github.chyuan_cuihongyuan.buzhou.guard.policy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-40 / spec 13 §T64 策略热加载：etag 条件拉取、快照原子替换、坏文件沿用旧版
 * 绝不部分生效、provenance（revision/activatedAt）进决策、轮询可关、解析失败定位。
 */
class PolicyRefresherTest {

    private static final PolicyDecision.Input READ_BY_TRUSTED =
            new PolicyDecision.Input("agent", "read_file", Map.of(), Map.of("taint", "TRUSTED"),
                    false);

    private static final PolicyDecision.Input READ_BY_UNTRUSTED =
            new PolicyDecision.Input("agent", "read_file", Map.of(), Map.of("taint", "UNTRUSTED"),
                    false);

    @TempDir
    Path tempDir;

    private Path policyFile(String json) throws Exception {
        Path file = tempDir.resolve("policy-" + System.nanoTime() + ".json");
        Files.writeString(file, json, StandardCharsets.UTF_8);
        return file;
    }

    @Test
    void parsesRulesAndDecidesWithProvenance() throws Exception {
        Path file = policyFile("""
                {"rules":[
                  {"id":"allow-trusted","toolPattern":"read_*",
                   "labels":[{"key":"taint","op":"eq","value":"TRUSTED"}],
                   "action":"ALLOW","reason":"受信读放行"},
                  {"id":"deny-rest","toolPattern":"*","action":"DENY","reason":"默认拒"}
                ]}""");
        try (PolicyRefresher refresher = new PolicyRefresher(
                new ResourcePolicySource("file:" + file), Duration.ZERO)) {
            assertThat(refresher.pollingEnabled()).isFalse();
            assertThat(refresher.ruleCount()).isEqualTo(2);

            PolicyDecision allowed = refresher.decide(READ_BY_TRUSTED);
            assertThat(allowed.action()).isEqualTo(PolicyDecision.Action.ALLOW);
            assertThat(allowed.reason()).contains("allow-trusted");
            assertThat(allowed.revision()).hasSize(64).isEqualTo(refresher.revision());
            assertThat(allowed.activatedAt()).isEqualTo(refresher.activatedAt());

            assertThat(refresher.decide(READ_BY_UNTRUSTED).action())
                    .isEqualTo(PolicyDecision.Action.DENY);
        }
    }

    @Test
    void hotReloadSwapsRulesAtomicallyAndEtavSkipsUnchanged() throws Exception {
        Path file = policyFile("""
                {"rules":[{"id":"deny-all","toolPattern":"*","action":"DENY","reason":"v1"}]}""");
        PolicySource source = new ResourcePolicySource("file:" + file);
        try (PolicyRefresher refresher = new PolicyRefresher(source, Duration.ZERO)) {
            assertThat(refresher.decide(READ_BY_TRUSTED).action())
                    .isEqualTo(PolicyDecision.Action.DENY);
            String revisionV1 = refresher.revision();

            // 同内容刷新 → 304 语义跳过（revision/activatedAt 不变）
            refresher.refresh();
            assertThat(refresher.refreshNoChangeCount()).isEqualTo(1);
            assertThat(refresher.revision()).isEqualTo(revisionV1);

            // 热更到 v2（allow）→ 原子替换，新决策即刻用新规则
            Files.writeString(file, """
                    {"rules":[{"id":"allow-all","toolPattern":"*","action":"ALLOW","reason":"v2"}]}""",
                    StandardCharsets.UTF_8);
            refresher.refresh();
            assertThat(refresher.refreshSuccessCount()).isEqualTo(1);
            assertThat(refresher.revision()).isNotEqualTo(revisionV1);
            PolicyDecision afterHotReload = refresher.decide(READ_BY_TRUSTED);
            assertThat(afterHotReload.action()).isEqualTo(PolicyDecision.Action.ALLOW);
            assertThat(afterHotReload.reason()).contains("allow-all");
            // provenance 跟随新快照
            assertThat(afterHotReload.revision()).isEqualTo(refresher.revision());
        }
    }

    @Test
    void badPolicyFileKeepsOldSnapshotNeverPartiallyApplied() throws Exception {
        Path file = policyFile("""
                {"rules":[{"id":"allow-all","toolPattern":"*","action":"ALLOW","reason":"good"}]}""");
        PolicySource source = new ResourcePolicySource("file:" + file);
        try (PolicyRefresher refresher = new PolicyRefresher(source, Duration.ZERO)) {
            // 坏 JSON（规则半解析）→ 沿用旧快照
            Files.writeString(file, "{\"rules\":[{\"id\":\"x\",\"action\":\"OOPS\"}]}",
                    StandardCharsets.UTF_8);
            refresher.refresh();
            assertThat(refresher.refreshFailureCount()).isEqualTo(1);
            assertThat(refresher.ruleCount()).isEqualTo(1);
            assertThat(refresher.decide(READ_BY_UNTRUSTED).action())
                    .isEqualTo(PolicyDecision.Action.ALLOW); // 旧规则继续生效

            // 来源消失 → 同样沿用
            Files.delete(file);
            refresher.refresh();
            assertThat(refresher.refreshFailureCount()).isEqualTo(2);
            assertThat(refresher.decide(READ_BY_UNTRUSTED).action())
                    .isEqualTo(PolicyDecision.Action.ALLOW);
        }
    }

    @Test
    void initialLoadFailureFailsFastWithExplicitMessage() {
        PolicySource missing = new ResourcePolicySource(
                "file:" + tempDir.resolve("no-such-policy.json"));
        assertThatThrownBy(() -> new PolicyRefresher(missing, Duration.ZERO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no-such-policy");
    }

    @Test
    void parserRejectsMalformedRulesWithPositions() {
        assertThatThrownBy(() -> PolicyRuleParser.parse("not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("合法 JSON");
        assertThatThrownBy(() -> PolicyRuleParser.parse("{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rules");
        assertThatThrownBy(() -> PolicyRuleParser.parse(
                "{\"rules\":[{\"id\":\"a\",\"action\":\"MAYBE\"}]}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("action 非法");
        assertThatThrownBy(() -> PolicyRuleParser.parse(
                "{\"rules\":[{\"id\":\"a\",\"action\":\"ALLOW\",\"labels\":[{\"key\":\"k\",\"op\":\"regex\"}]}]}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("op 非法");
        // 合法最小集 + 缺省 toolPattern = *
        List<PolicyDecision.Rule> rules = PolicyRuleParser.parse(
                "{\"rules\":[{\"id\":\"a\",\"action\":\"ESCALATE\"}]}");
        assertThat(rules).hasSize(1);
        assertThat(rules.getFirst().toolPattern()).isEqualTo("*");
        assertThat(EmbeddedPolicyEngine.matchesTool(rules.getFirst().toolPattern(), "anything"))
                .isTrue();
    }

    @Test
    void scheduledPollingRefreshesInBackground() throws Exception {
        Path file = policyFile("""
                {"rules":[{"id":"deny-all","toolPattern":"*","action":"DENY","reason":"v1"}]}""");
        PolicySource source = new ResourcePolicySource("file:" + file);
        try (PolicyRefresher refresher = new PolicyRefresher(source, Duration.ofMillis(50))) {
            assertThat(refresher.pollingEnabled()).isTrue();
            Files.writeString(file, """
                    {"rules":[{"id":"allow-all","toolPattern":"*","action":"ALLOW","reason":"v2"}]}""",
                    StandardCharsets.UTF_8);
            long deadline = System.currentTimeMillis() + 5_000;
            while (refresher.decide(READ_BY_TRUSTED).action() != PolicyDecision.Action.ALLOW
                    && System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
            }
            assertThat(refresher.decide(READ_BY_TRUSTED).action())
                    .isEqualTo(PolicyDecision.Action.ALLOW);
            assertThat(refresher.refreshSuccessCount()).isGreaterThanOrEqualTo(1);
        }
    }
}
