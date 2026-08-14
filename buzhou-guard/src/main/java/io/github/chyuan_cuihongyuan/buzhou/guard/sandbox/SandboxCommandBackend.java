package io.github.chyuan_cuihongyuan.buzhou.guard.sandbox;

import io.github.chyuan_cuihongyuan.buzhou.core.exec.CommandBackend;

import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CommandBackend 桥接（spec 17 / T85 / impl-60）：把 guard 的 {@link CommandSandbox} 档
 * （Deno / E2B / Firecracker / Limited 组合，由应用注册）适配为 core SPI，供 tools 的
 * run_command 沙箱委托版装配消费。档位选择归应用（注册什么 CommandSandbox bean 就桥什么）。
 *
 * <p>环境白名单：仅 PATH/HOME/LANG/TZ 且只在父环境存在时透传（与 tools BASE_ENV_ALLOWLIST
 * 子集口径一致——沙箱侧隔离更强，白名单从紧）。沙箱不可用抛
 * {@link IllegalStateException}（附 unavailableHint），不静默回退。
 */
public final class SandboxCommandBackend implements CommandBackend {

    private static final List<String> ENV_ALLOWLIST = List.of("PATH", "HOME", "LANG", "TZ");

    private final CommandSandbox sandbox;

    public SandboxCommandBackend(CommandSandbox sandbox) {
        this.sandbox = sandbox;
    }

    @Override
    public String name() {
        return "guard-" + sandbox.name();
    }

    @Override
    public CommandOutcome run(String shellCommand, Path workDir, long timeoutSeconds) {
        if (!sandbox.available()) {
            throw new IllegalStateException(sandbox.unavailableHint());
        }
        CommandSandbox.CommandResult result = sandbox.run(
                List.of("/bin/sh", "-c", shellCommand),
                allowlistedEnv(),
                workDir,
                Duration.ofSeconds(timeoutSeconds));
        boolean truncated = result.truncated()
                || result.killedReason() == CommandSandbox.CommandResult.KilledReason.OUTPUT;
        return new CommandOutcome(result.exitCode(), result.stdout(), result.stderr(),
                result.timedOut(), truncated);
    }

    private static Map<String, String> allowlistedEnv() {
        Map<String, String> env = new LinkedHashMap<>();
        for (String name : ENV_ALLOWLIST) {
            String value = System.getenv(name);
            if (value != null) {
                env.put(name, value);
            }
        }
        return env;
    }
}
