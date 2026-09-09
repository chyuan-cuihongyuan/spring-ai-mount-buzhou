package io.github.chyuan_cuihongyuan.buzhou.guard.secret;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultTurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 418 §Testing / T727–T728：秘密命中统计——三侧计数与快照排序；hook
 * 三缝真实记账；JSONL 追加两轮快照。
 */
class SecretHitStatsTest {

    @Test
    void shouldCountByTypeAndSide_withSortedSnapshot() {
        SecretHitStats.install(null); // 清零
        SecretHitStats stats = SecretHitStats.global();
        stats.record(SecretType.AWS_ACCESS_KEY, SecretHitStats.Side.INPUT);
        stats.record(SecretType.AWS_ACCESS_KEY, SecretHitStats.Side.INPUT);
        stats.record(SecretType.GITHUB_TOKEN, SecretHitStats.Side.OUTBOUND);
        stats.record(SecretType.JWT, SecretHitStats.Side.OUTPUT);

        List<SecretHitStats.Hit> snapshot = stats.snapshot();
        assertThat(snapshot).hasSize(3); // 3 类型（AWS 同侧 2 次合 1 行）
        assertThat(snapshot).containsExactly(
                new SecretHitStats.Hit("AWS_ACCESS_KEY", SecretHitStats.Side.INPUT, 2),
                new SecretHitStats.Hit("GITHUB_TOKEN", SecretHitStats.Side.OUTBOUND, 1),
                new SecretHitStats.Hit("JWT", SecretHitStats.Side.OUTPUT, 1));
        // 名字典序 + 侧序
        assertThat(snapshot.get(0).name()).isLessThan(snapshot.get(1).name());
    }

    @Test
    void shouldRecordThroughHookSeams_atCorrectSides(@TempDir Path dir) throws Exception {
        SecretHitStats.install(null);
        SecretScanHook hook = new SecretScanHook();
        HookEnvironment env = new HookEnvironment("s", "a", new InMemorySessionStateStore());

        // INPUT 缝：用户粘贴 AWS key
        DefaultTurnContext turn = new DefaultTurnContext(env, "我的 AKIAIOSFODNN7EXAMPLE");
        hook.beforeTurn(turn);
        // OUTBOUND 缝：出站参数带 GitHub token
        DefaultToolCallContext outbound = new DefaultToolCallContext(env, "c1", "http_post",
                Map.of("body", "token=ghp_16CharactersTOKEN1234567890"));
        hook.beforeTool(outbound);
        // OUTPUT 缝：结果带 JWT
        DefaultToolCallContext inbound = new DefaultToolCallContext(env, "c2", "read", Map.of());
        inbound.replaceResult("看这个 eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.abcde-_fGhI");
        hook.afterTool(inbound);

        List<SecretHitStats.Hit> snapshot = SecretHitStats.global().snapshot();
        assertThat(snapshot).containsExactlyInAnyOrder(
                new SecretHitStats.Hit("AWS_ACCESS_KEY", SecretHitStats.Side.INPUT, 1),
                new SecretHitStats.Hit("GITHUB_TOKEN", SecretHitStats.Side.OUTBOUND, 1),
                new SecretHitStats.Hit("JWT", SecretHitStats.Side.OUTPUT, 1));

        // JSONL 两轮快照：每轮 3 行追加（不清零）
        Path jsonl = dir.resolve("secret-hits.jsonl");
        assertThat(SecretHitStatsJsonl.appendSnapshot(jsonl)).isEqualTo(3);
        assertThat(SecretHitStatsJsonl.appendSnapshot(jsonl)).isEqualTo(3);
        List<String> lines = Files.readAllLines(jsonl);
        assertThat(lines).hasSize(6);
        assertThat(lines.get(0)).contains("\"name\":\"AWS_ACCESS_KEY\"")
                .contains("\"side\":\"INPUT\"").contains("\"count\":1");
    }
}
