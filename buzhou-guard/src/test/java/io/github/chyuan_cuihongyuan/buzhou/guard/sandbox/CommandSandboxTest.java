package io.github.chyuan_cuihongyuan.buzhou.guard.sandbox;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-25 / T51 CommandSandbox：argv 构造契约（授权面/零 shell/独立 token）、
 * deny-by-default 环境白名单、探测式可用性与不可用指引、重载档预留语义。
 */
class CommandSandboxTest {

    /** 假执行器：记录 argv、可编程返回。 */
    private static final class RecordingLauncher implements SandboxProcessLauncher {
        final List<List<String>> invocations = new CopyOnWriteArrayList<>();
        boolean succeedVersionProbe = true;

        @Override
        public CommandSandbox.CommandResult launch(List<String> argv, Map<String, String> env,
                                                   Path workDir, Duration timeout) {
            invocations.add(List.copyOf(argv));
            boolean versionProbe = argv.size() == 2 && argv.get(1).equals("--version");
            // 探测调用按可编程旗标；真实执行调用恒成功（本测试断言构造契约而非进程行为）
            return new CommandSandbox.CommandResult(versionProbe && !succeedVersionProbe ? 1 : 0,
                    "out", "err", false);
        }
    }

    @Test
    void sandboxCommandCarriesPermitsAndTokensAsArgv() {
        RecordingLauncher launcher = new RecordingLauncher();
        DenoSandbox sandbox = DenoSandbox.builder(launcher)
                .allowRead("/data")
                .allowNet("api.internal:443")
                .allowEnv("API_TOKEN")
                .allowRun("python3")
                .build();

        List<String> argv = sandbox.sandboxCommand(List.of("python3", "script.py",
                "arg with spaces; rm -rf /"));
        // 形状：deno eval --no-prompt <授权面> <固定脚本> -- <独立 token...>
        assertThat(argv.get(0)).isEqualTo("deno");
        assertThat(argv.get(1)).isEqualTo("eval");
        assertThat(argv.get(2)).isEqualTo("--no-prompt");
        assertThat(String.join(" ", argv)).contains("--allow-read=/data")
                .contains("--allow-net=api.internal:443")
                .contains("--allow-env=API_TOKEN")
                .contains("--allow-run=python3");
        // 固定启动脚本在 -- 之前（常量、无用户内容）
        int separator = argv.indexOf("--");
        assertThat(separator).isGreaterThan(0);
        assertThat(argv.get(separator - 1)).isEqualTo(DenoSandbox.BOOT_SCRIPT);
        // 用户命令为独立 argv token（含空格/元字符原样保留，零 shell 拼接）
        assertThat(argv.subList(separator + 1, argv.size()))
                .containsExactly("python3", "script.py", "arg with spaces; rm -rf /");

        // run 走同一构造 + 白名单环境透传
        var result = sandbox.run(List.of("python3", "x.py"), Map.of("API_TOKEN", "secret"),
                Path.of("/data"), Duration.ofSeconds(5));
        assertThat(result.success()).isTrue();
        assertThat(launcher.invocations).hasSize(2); // 探测 + 执行
        assertThat(launcher.invocations.get(1).containsAll(List.of("python3", "x.py"))).isTrue();
    }

    @Test
    void unavailableProbeGivesExplicitHintInsteadOfSilentFallback() {
        RecordingLauncher brokenLauncher = new RecordingLauncher();
        brokenLauncher.succeedVersionProbe = false;
        DenoSandbox sandbox = DenoSandbox.builder(brokenLauncher).build();
        assertThat(sandbox.available()).isFalse();
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> sandbox.run(List.of("ls"), Map.of(), null, Duration.ofSeconds(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Deno");
    }

    @Test
    void heavyTiersAreReservedWithDeploymentHints() {
        FirecrackerSandbox firecracker = new FirecrackerSandbox();
        E2BSandbox e2b = new E2BSandbox();
        // 非 Linux/无 KVM 本机：探测为不可用并给指引；实现按部署需求（接口预留语义）
        assertThat(firecracker.name()).isEqualTo("firecracker");
        assertThat(firecracker.unavailableHint()).contains("KVM");
        assertThat(e2b.name()).isEqualTo("e2b");
        assertThat(e2b.unavailableHint()).contains("E2B_API_KEY");
        if (!firecracker.available()) {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> firecracker.run(
                            List.of("ls"), Map.of(), null, Duration.ofSeconds(1)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
