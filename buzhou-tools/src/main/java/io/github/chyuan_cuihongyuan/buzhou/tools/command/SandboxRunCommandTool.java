package io.github.chyuan_cuihongyuan.buzhou.tools.command;

import io.github.chyuan_cuihongyuan.buzhou.core.exec.CommandBackend;
import io.github.chyuan_cuihongyuan.buzhou.core.fs.FileSandbox;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.nio.file.Path;
import java.time.Duration;

/**
 * run_command 沙箱委托版（spec 17 / T85 / impl-60）：与内置 {@link RunCommandTool} 同名同 Schema，
 * <b>装配期二选一</b>——{@code buzhou.tools.command.backend=sandbox} 且容器存在
 * {@link CommandBackend}（guard 模块 {@code SandboxCommandBackend} 桥接）时注册本类，
 * 否则注册内置 ProcessBuilder 版。
 *
 * <p>分工：前置校验（命令黑名单 / workdir 逐段防线 / 超时范围）在本类，<b>执行隔离</b>
 * （环境白名单、进程树击杀、输出限额）归 backend 沙箱实现。沙箱运行时不可用不静默回退裸执行。
 * 入参契约见 {@link RunCommandArgs}。
 */
@BuzhouTool(name = "run_command")
public class SandboxRunCommandTool implements ToolCallback {

    /** workdir 单段白名单：字母/数字/_-. 与中文/空格；斜杠由切段处理，{@code ..} 显式拒绝。 */
    private static final java.util.regex.Pattern SAFE_WORKDIR_SEGMENT =
            java.util.regex.Pattern.compile("[\\w\\-. \\u4e00-\\u9fa5]+");

    private final FileSandbox sandbox;
    private final CommandBlacklist blacklist;
    private final CommandBackend backend;
    private final Duration defaultTimeout;
    private final Duration maxTimeout;

    public SandboxRunCommandTool(FileSandbox sandbox, CommandBlacklist blacklist,
                                 CommandBackend backend, Duration defaultTimeout, Duration maxTimeout) {
        this.sandbox = sandbox;
        this.blacklist = blacklist == null ? CommandBlacklist.defaults() : blacklist;
        this.backend = backend;
        this.defaultTimeout = defaultTimeout;
        this.maxTimeout = maxTimeout;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name("run_command")
                .description("在沙箱内执行 shell 命令（黑名单命令拒绝执行；执行经强隔离沙箱 backend）。"
                        + "工作目录不得越出沙箱。")
                .inputSchema("""
                        {"type":"object","properties":{
                          "command":{"type":"string","description":"命令行；命中黑名单即拒"},
                          "workdir":{"type":"string","description":"工作目录，默认沙箱 root，不得越界"},
                          "timeoutSeconds":{"type":"integer","description":"超时秒数，默认 60，上限可配"}
                        },"required":["command"]}
                        """)
                .build();
    }

    @Override
    public String call(String toolInput) {
        try {
            RunCommandArgs args = RunCommandArgs.parse(toolInput);
            if (args.command().isBlank()) {
                return "run_command 失败：command 不能为空";
            }
            if (blacklist.matches(args.command())) {
                return "run_command 拒绝：命令命中安全黑名单";
            }
            Path workdir = resolveWorkdir(args.workdir());
            if (!java.nio.file.Files.isDirectory(workdir)) {
                return "run_command 失败：工作目录不存在：" + workdir;
            }
            long timeoutSeconds = args.timeoutSeconds() > 0
                    ? args.timeoutSeconds() : defaultTimeout.toSeconds();
            if (timeoutSeconds <= 0 || timeoutSeconds > maxTimeout.toSeconds()) {
                return "run_command 失败：timeoutSeconds 超出允许范围（1~" + maxTimeout.toSeconds() + "）";
            }
            return dispatch(workdir, timeoutSeconds, args.command());
        } catch (Exception e) {
            return "run_command 失败：" + e.getMessage();
        }
    }

    /**
     * workdir 解析逐段防线：①按 '/' 切段；②每段过严格 token 白名单，显式拒绝 {@code ..}；
     * ③在沙箱 root 上逐段拼接（resolve 只收已验证短段）；④最终必须仍位于 root 内。
     */
    private Path resolveWorkdir(String workdirRaw) {
        Path root = sandbox.root().normalize();
        if (workdirRaw.isBlank()) {
            return root;
        }
        Path workdir = root;
        for (String segment : workdirRaw.split("/")) {
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment) || !SAFE_WORKDIR_SEGMENT.matcher(segment).matches()) {
                throw new IllegalArgumentException("workdir 含非法路径段，已拒绝");
            }
            workdir = workdir.resolve(segment);
        }
        if (!workdir.normalize().startsWith(root)) {
            throw new IllegalArgumentException("workdir 越出沙箱边界，已拒绝");
        }
        return workdir.normalize();
    }

    /** 委托执行：不可用不静默回退裸执行；结果显式带 exit/stderr/truncated/超时标记。 */
    private String dispatch(Path workdir, long timeoutSeconds, String validatedCommand) {
        final CommandBackend.CommandOutcome outcome;
        try {
            outcome = backend.run(validatedCommand, workdir, timeoutSeconds);
        } catch (IllegalStateException unavailable) {
            return String.format("run_command 失败：命令沙箱不可用（backend=%s）：%s——已拒绝裸执行回退",
                    backend.name(), unavailable.getMessage());
        }
        String prefix = outcome.timedOut()
                ? String.format("run_command 超时（%ds，backend=%s），进程已终止%n", timeoutSeconds, backend.name())
                : outcome.exitCode() == 0 ? "" : String.format("exit=%d%n", outcome.exitCode());
        String stderrSection = outcome.stderr().isBlank()
                ? "" : String.format("%nstderr:%n%s", outcome.stderr());
        String truncatedNote = outcome.truncated()
                ? String.format("%n[输出超出沙箱限额被截断（backend=%s）]", backend.name()) : "";
        return prefix + outcome.stdout() + stderrSection + truncatedNote;
    }
}
