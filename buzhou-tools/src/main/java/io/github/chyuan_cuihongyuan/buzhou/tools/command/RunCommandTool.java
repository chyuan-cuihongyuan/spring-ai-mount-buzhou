package io.github.chyuan_cuihongyuan.buzhou.tools.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.fs.FileSandbox;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * run_command — 沙箱内执行命令（危险，默认关、绑定级 opt-in、默认挂 HITL 守卫）。
 *
 * <p>安全边界全部默认开：命令黑名单（命中即拒）+ workdir 限沙箱 + 超时（默认 60s，
 * 上限可配）。经 {@code /bin/sh -c} 执行以支持管道/重定向；输出超限由 Spill 管道治理，
 * 本工具只设 5MB 内存兜底截断（防 OOM，非上下文治理）。
 *
 * <p><b>跨平台约束（spec 06 / impl 07）</b>：命令经 {@code /bin/sh -c} 执行，需 POSIX
 * shell 环境——Linux / macOS 原生可用；Windows 需 WSL 或 Git Bash 提供 {@code /bin/sh}。
 * 因 run_command <b>默认关</b>（{@code buzhou.tools.run-command.enabled=false}，仅显式
 * opt-in 才注册并挂 HITL 守卫），此跨平台差异只影响显式开启者，不影响开箱即用的安全默认。
 */
@BuzhouTool(name = "run_command")
public class RunCommandTool implements ToolCallback {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** 输出内存兜底上限（5MB）；上下文治理归 Spill offload，不在此截断语义内（spec 06 推演 #14）。 */
    private static final int MAX_OUTPUT_BYTES = 5 * 1024 * 1024;
    /** 主进程退出后排空输出管道的等待上限：逾期视为分离子进程悬挂，强杀整棵进程树。 */
    private static final long OUTPUT_DRAIN_GRACE_MILLIS = 2000;

    private final FileSandbox sandbox;
    private final CommandBlacklist blacklist;
    private final Duration defaultTimeout;
    private final Duration maxTimeout;

    public RunCommandTool(FileSandbox sandbox, CommandBlacklist blacklist,
                          Duration defaultTimeout, Duration maxTimeout) {
        this.sandbox = sandbox;
        this.blacklist = blacklist;
        this.defaultTimeout = defaultTimeout;
        this.maxTimeout = maxTimeout;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name("run_command")
                .description("在沙箱内执行 shell 命令（黑名单命令拒绝执行）。工作目录不得越出沙箱。")
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
            JsonNode args = MAPPER.readTree(toolInput);
            String command = args.path("command").asText("");
            if (command.isBlank()) {
                return "run_command 失败：command 不能为空";
            }
            if (blacklist.matches(command)) {
                return "run_command 拒绝：命令命中安全黑名单";
            }
            String workdirRaw = args.path("workdir").asText("");
            Path workdir = workdirRaw.isBlank() ? sandbox.root() : sandbox.resolve(workdirRaw);
            if (!java.nio.file.Files.isDirectory(workdir)) {
                return "run_command 失败：工作目录不存在：" + workdir;
            }
            long timeoutSeconds = args.path("timeoutSeconds").asLong(defaultTimeout.toSeconds());
            if (timeoutSeconds <= 0 || timeoutSeconds > maxTimeout.toSeconds()) {
                return "run_command 失败：timeoutSeconds 超出允许范围（1~" + maxTimeout.toSeconds() + "）";
            }
            return execute(command, workdir, timeoutSeconds);
        } catch (Exception e) {
            return "run_command 失败：" + e.getMessage();
        }
    }

    private String execute(String command, Path workdir, long timeoutSeconds)
            throws IOException, InterruptedException {
        Process process = new ProcessBuilder("/bin/sh", "-c", command)
                .directory(workdir.toFile())
                .redirectErrorStream(true)
                .start();
        // 异步读输出防管道缓冲区满死锁；读线程用虚拟线程，不占公共 ForkJoinPool
        // （分离子进程可能长持管道，读线程阻塞时长不受控）
        CompletableFuture<String> outputFuture = CompletableFuture.supplyAsync(
                () -> readBounded(process.getInputStream()),
                r -> Thread.ofVirtual().start(r));
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            // 超时强杀整棵进程树（destroyForcibly 只杀 sh 本身，后台子进程会成孤儿悬挂管道）
            killProcessTree(process);
            return "run_command 超时（" + timeoutSeconds + "s），进程已终止\n"
                    + drainOutput(outputFuture, process);
        }
        String output = drainOutput(outputFuture, process);
        int exit = process.exitValue();
        return (exit == 0 ? "" : "exit=" + exit + "\n") + output;
    }

    /** 排空输出管道；逾期（分离子进程悬挂管道）则强杀整棵进程树后再短等一次兜底。 */
    private static String drainOutput(CompletableFuture<String> outputFuture, Process process)
            throws InterruptedException {
        try {
            return outputFuture.get(OUTPUT_DRAIN_GRACE_MILLIS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            killProcessTree(process);
            try {
                return outputFuture.get(OUTPUT_DRAIN_GRACE_MILLIS, TimeUnit.MILLISECONDS)
                        + "\n[检测到分离子进程悬挂输出管道，已强杀进程树]";
            } catch (TimeoutException | ExecutionException e2) {
                return "[输出管道被悬挂子进程占用，已强杀进程树并放弃读取]";
            }
        } catch (ExecutionException e) {
            return "[读取进程输出失败：" + e.getCause().getMessage() + "]";
        }
    }

    private static void killProcessTree(Process process) {
        process.toHandle().descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    private static String readBounded(InputStream in) {
        try (in) {
            byte[] buf = in.readNBytes(MAX_OUTPUT_BYTES + 1);
            if (buf.length > MAX_OUTPUT_BYTES) {
                return new String(buf, 0, MAX_OUTPUT_BYTES, StandardCharsets.UTF_8)
                        + "\n[输出超过内存兜底上限 5MB，已截断]";
            }
            return new String(buf, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "[读取进程输出失败：" + e.getMessage() + "]";
        }
    }
}
