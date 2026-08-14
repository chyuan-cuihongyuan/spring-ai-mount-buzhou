package io.github.chyuan_cuihongyuan.buzhou.guard.sandbox;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-40 / spec 13 §T64 沙箱限额：输出超限截断显式标记（truncated/killedReason）、
 * 超时上界取更小者并归因 TIMEOUT、Deno 探测缓存（TTL 内单探测、失效重探）。
 */
class SandboxLimitsTest {

    /** 可编程假执行器：记录调用、可配置输出与超时映射。 */
    private static final class ScriptedLauncher implements SandboxProcessLauncher {
        final List<Duration> timeouts = new CopyOnWriteArrayList<>();
        int calls;
        String stdout = "out".repeat(10);
        boolean timedOut;

        @Override
        public CommandSandbox.CommandResult launch(List<String> argv,
                Map<String, String> env, Path workDir, Duration timeout) {
            calls++;
            timeouts.add(timeout);
            boolean versionProbe = argv.size() == 2 && argv.get(1).equals("--version");
            if (versionProbe) {
                return new CommandSandbox.CommandResult(0, "deno 2.0", "", false);
            }
            return new CommandSandbox.CommandResult(timedOut ? 137 : 0, stdout, "err",
                    timedOut);
        }
    }

    @Test
    void oversizedOutputIsTruncatedAndExplicitlyMarked() {
        ScriptedLauncher launcher = new ScriptedLauncher();
        // 每字符 1 字节（ASCII），stdout 30 字节 > 上界 16
        LimitedCommandSandbox sandbox = new LimitedCommandSandbox(
                DenoSandbox.builder(launcher).build(),
                new SandboxLimits(null, 16L, null));

        CommandSandbox.CommandResult result = sandbox.run(List.of("cat", "big.txt"),
                Map.of(), null, Duration.ofSeconds(5));

        assertThat(result.truncated()).isTrue();
        assertThat(result.killedReason())
                .isEqualTo(CommandSandbox.CommandResult.KilledReason.OUTPUT);
        assertThat(result.stdout().getBytes(StandardCharsets.UTF_8)).hasSize(16);
        // stderr 同样受限（各自截断）
        assertThat(result.stderr()).isEqualTo("err");
        assertThat(result.success()).isTrue(); // 截断非失败：进程正常退出
    }

    @Test
    void utf8BoundarySafeTruncation() {
        // 3 字节/字符的中文：9 字节上界切在字符边界（3 字符），无坏尾
        LimitedCommandSandbox.Truncated truncated =
                LimitedCommandSandbox.truncate("不周山不周山", 9);
        assertThat(truncated.truncated()).isTrue();
        assertThat(truncated.value()).isEqualTo("不周山");
        LimitedCommandSandbox.Truncated exact =
                LimitedCommandSandbox.truncate("不周山", 9);
        assertThat(exact.truncated()).isFalse();
        assertThat(exact.value()).isEqualTo("不周山");
    }

    @Test
    void timeoutCapTakesTheSmallerAndMapsTimeoutKillReason() {
        ScriptedLauncher launcher = new ScriptedLauncher();
        launcher.timedOut = true;
        LimitedCommandSandbox sandbox = new LimitedCommandSandbox(
                DenoSandbox.builder(launcher).build(),
                new SandboxLimits(Duration.ofSeconds(2), null, null));

        CommandSandbox.CommandResult result = sandbox.run(List.of("sleep", "100"),
                Map.of(), null, Duration.ofSeconds(60));

        // 生效超时 = min(60s, 2s) = 2s；执行超时被击杀 → killedReason=TIMEOUT
        assertThat(launcher.timeouts.getLast()).isEqualTo(Duration.ofSeconds(2));
        assertThat(result.timedOut()).isTrue();
        assertThat(result.killedReason())
                .isEqualTo(CommandSandbox.CommandResult.KilledReason.TIMEOUT);
        // 调用方更短时尊重调用方
        sandbox.run(List.of("sleep", "1"), Map.of(), null, Duration.ofSeconds(1));
        assertThat(launcher.timeouts.getLast()).isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    void withinLimitsResultPassesThroughUnchanged() {
        ScriptedLauncher launcher = new ScriptedLauncher();
        LimitedCommandSandbox sandbox = new LimitedCommandSandbox(
                DenoSandbox.builder(launcher).build(),
                new SandboxLimits(Duration.ofSeconds(30), 1024L, null));
        CommandSandbox.CommandResult result = sandbox.run(List.of("echo", "hi"), Map.of(),
                null, Duration.ofSeconds(10));
        assertThat(result.truncated()).isFalse();
        assertThat(result.killedReason()).isNull();
        assertThat(result.success()).isTrue();
    }

    @Test
    void denoProbeIsCachedWithinTtlAndReprobedAfterInvalidation() {
        ScriptedLauncher launcher = new ScriptedLauncher();
        DenoSandbox sandbox = DenoSandbox.builder(launcher).build();

        assertThat(sandbox.available()).isTrue();
        int afterFirstProbe = launcher.calls;
        // TTL（默认 60s）内多次 available/run 不再重复探测
        assertThat(sandbox.available()).isTrue();
        sandbox.run(List.of("ls"), Map.of(), null, Duration.ofSeconds(1));
        assertThat(launcher.calls).isEqualTo(afterFirstProbe + 1); // 仅那次 run 的执行调用

        // 显式失效 → 重探
        sandbox.invalidateProbeCache();
        assertThat(sandbox.available()).isTrue();
        assertThat(launcher.calls).isEqualTo(afterFirstProbe + 2);

        // TTL=0 → 每次实探
        DenoSandbox noCache = DenoSandbox.builder(launcher).probeTtl(Duration.ZERO).build();
        noCache.available();
        noCache.available();
        // 两次 available = 两次探测
        assertThat(launcher.calls).isGreaterThanOrEqualTo(afterFirstProbe + 4);
    }

    @Test
    void buildLimitedWrapsWhenLimitsPresent() {
        ScriptedLauncher launcher = new ScriptedLauncher();
        CommandSandbox limited = DenoSandbox.builder(launcher)
                .limits(new SandboxLimits(null, 4L, null))
                .buildLimited();
        assertThat(limited).isInstanceOf(LimitedCommandSandbox.class);
        CommandSandbox.CommandResult result = limited.run(List.of("cat", "f"), Map.of(), null,
                Duration.ofSeconds(1));
        assertThat(result.truncated()).isTrue();

        // limits=NONE 时 buildLimited 等价裸档
        CommandSandbox raw = DenoSandbox.builder(limiterOfNone()).buildLimited();
        assertThat(raw).isInstanceOf(DenoSandbox.class);
    }

    private SandboxProcessLauncher limiterOfNone() {
        return new ScriptedLauncher();
    }
}
