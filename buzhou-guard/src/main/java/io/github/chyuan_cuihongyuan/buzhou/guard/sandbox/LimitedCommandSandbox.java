package io.github.chyuan_cuihongyuan.buzhou.guard.sandbox;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 限额装饰器（impl-40 / spec 13 §T64）：任意 {@link CommandSandbox} 包一层
 * {@link SandboxLimits}——超时取更小者、输出超限截断并显式标记、内存限额透传部署侧执行器。
 *
 * <p>截断语义：stdout/stderr <b>各自</b>按 {@code maxOutputBytes} 截断（UTF-8 边界安全，
 * 尾部坏字节以替换符呈现）；一旦任一流被截断即 {@code truncated=true} +
 * {@code killedReason=OUTPUT}。超时击杀由被装饰沙箱的 timedOut 透出并归因 TIMEOUT。
 */
public final class LimitedCommandSandbox implements CommandSandbox {

    private static final Logger LOG = LoggerFactory.getLogger(LimitedCommandSandbox.class);

    private final CommandSandbox delegate;
    private final SandboxLimits limits;

    public LimitedCommandSandbox(CommandSandbox delegate, SandboxLimits limits) {
        this.delegate = delegate;
        this.limits = limits == null ? SandboxLimits.NONE : limits;
    }

    public SandboxLimits limits() {
        return limits;
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public boolean available() {
        return delegate.available();
    }

    @Override
    public String unavailableHint() {
        return delegate.unavailableHint();
    }

    @Override
    public CommandResult run(List<String> command, Map<String, String> allowedEnv, Path workDir,
            Duration timeout) {
        Duration effectiveTimeout = effectiveTimeout(timeout);
        CommandResult raw = delegate.run(command, allowedEnv, workDir, effectiveTimeout);
        return applyLimits(raw, command);
    }

    private Duration effectiveTimeout(Duration callerTimeout) {
        if (limits.timeout() == null) {
            return callerTimeout;
        }
        if (callerTimeout == null) {
            return limits.timeout();
        }
        return callerTimeout.compareTo(limits.timeout()) <= 0 ? callerTimeout : limits.timeout();
    }

    private CommandResult applyLimits(CommandResult raw, List<String> command) {
        boolean truncated = false;
        String stdout = raw.stdout();
        String stderr = raw.stderr();
        long max = limits.maxOutputBytes() == null ? 0 : limits.maxOutputBytes();
        if (max > 0) {
            Truncated out = truncate(stdout, max);
            Truncated err = truncate(stderr, max);
            truncated = out.truncated() || err.truncated();
            stdout = out.value();
            stderr = err.value();
        }
        CommandSandbox.CommandResult.KilledReason reason = raw.killedReason();
        if (raw.timedOut() && reason == null) {
            reason = CommandSandbox.CommandResult.KilledReason.TIMEOUT;
        }
        if (truncated) {
            reason = CommandSandbox.CommandResult.KilledReason.OUTPUT;
            LOG.warn("沙箱输出超限截断（sandbox={}，maxOutputBytes={}，command 头部={}）",
                    delegate.name(), max, command.isEmpty() ? "" : command.getFirst());
        }
        return new CommandResult(raw.exitCode(), stdout, stderr, raw.timedOut(), truncated,
                reason);
    }

    /** UTF-8 边界安全截断（坏尾字节以替换符呈现）。 */
    static Truncated truncate(String value, long maxBytes) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) {
            return new Truncated(value, false);
        }
        // 回退最多 3 字节避开被切断的多字节序列尾部
        int cut = (int) maxBytes;
        while (cut > 0 && (bytes[cut] & 0xC0) == 0x80) {
            cut--;
        }
        return new Truncated(new String(bytes, 0, cut, StandardCharsets.UTF_8), true);
    }

    record Truncated(String value, boolean truncated) {
    }
}
