package io.github.chyuan_cuihongyuan.buzhou.tools.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.fs.FileSandbox;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.io.ByteArrayOutputStream;
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
    /** 缺省输出内存兜底上限（5MB）；可配覆写（spec 43 §A / T157）。 */
    public static final long DEFAULT_MAX_OUTPUT_BYTES = 5L * 1024 * 1024;
    /** 主进程退出后继续排空在途输出字节的宽限；逾期即返回已捕获内容，不阻塞在分离子进程持有的管道上。 */
    private static final long OUTPUT_DRAIN_GRACE_MILLIS = 500;
    /** 主进程存活 / 宽限期内复检输出管道的轮询间隔。 */
    private static final long POLL_MILLIS = 10;

    /** impl-49：子进程环境变量白名单基线——父进程其余环境（DB 密码/API key 等）不透传给模型驱动的 shell。 */
    static final java.util.Set<String> BASE_ENV_ALLOWLIST = java.util.Set.of(
            "PATH", "HOME", "LANG", "LC_ALL", "TZ", "TERM");

    private final FileSandbox sandbox;
    private final CommandBlacklist blacklist;
    private final Duration defaultTimeout;
    private final Duration maxTimeout;
    /** impl-49：BASE 之外显式追加透传的环境变量名（大小写敏感，ProcessBuilder 语义）。 */
    private final java.util.Set<String> extraEnvAllowlist;
    /** impl-60：沙箱委托（spec 17 合流）——非 null 时执行走 backend，null 走内置 ProcessBuilder。 */
    private final io.github.chyuan_cuihongyuan.buzhou.core.exec.CommandBackend backend;
    /** spec 43 §A / T157 / impl-128：输出内存兜底上限（可配；缺省 5MB）。 */
    private final long maxOutputBytes;

    public RunCommandTool(FileSandbox sandbox, CommandBlacklist blacklist,
                          Duration defaultTimeout, Duration maxTimeout) {
        this(sandbox, blacklist, defaultTimeout, maxTimeout, java.util.Set.of());
    }

    public RunCommandTool(FileSandbox sandbox, CommandBlacklist blacklist,
                          Duration defaultTimeout, Duration maxTimeout,
                          java.util.Set<String> extraEnvAllowlist) {
        this(sandbox, blacklist, defaultTimeout, maxTimeout, extraEnvAllowlist, null);
    }

    /** 沙箱委托构造（spec 17 / impl-60）：黑名单与 workdir 前置校验不变，执行隔离交 backend。 */
    public RunCommandTool(FileSandbox sandbox, CommandBlacklist blacklist,
                          Duration defaultTimeout, Duration maxTimeout,
                          java.util.Set<String> extraEnvAllowlist,
                          io.github.chyuan_cuihongyuan.buzhou.core.exec.CommandBackend backend) {
        this(sandbox, blacklist, defaultTimeout, maxTimeout, extraEnvAllowlist, backend,
                DEFAULT_MAX_OUTPUT_BYTES);
    }

    /**
     * spec 43 §A / T157 / impl-128：输出内存兜底上限可配构造（正数；缺省 {@link #DEFAULT_MAX_OUTPUT_BYTES}=5MB）。
     * 截断语义：读线程捕获到上限即返回并附截断标记（进程继续跑完、exit 码照常上报）。
     */
    public RunCommandTool(FileSandbox sandbox, CommandBlacklist blacklist,
                          Duration defaultTimeout, Duration maxTimeout,
                          java.util.Set<String> extraEnvAllowlist,
                          io.github.chyuan_cuihongyuan.buzhou.core.exec.CommandBackend backend,
                          long maxOutputBytes) {
        if (maxOutputBytes <= 0) {
            throw new IllegalArgumentException(
                    "maxOutputBytes 必须为正（收到 " + maxOutputBytes + "）");
        }
        this.sandbox = sandbox;
        this.blacklist = blacklist;
        this.defaultTimeout = defaultTimeout;
        this.maxOutputBytes = maxOutputBytes;
        this.maxTimeout = maxTimeout;
        this.extraEnvAllowlist = extraEnvAllowlist == null ? java.util.Set.of() : extraEnvAllowlist;
        this.backend = backend;
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
        ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", command)
                .directory(workdir.toFile())
                .redirectErrorStream(true);
        applyEnvAllowlist(builder);
        Process process = builder.start();
        // 异步排空输出防管道缓冲区满死锁；读线程用虚拟线程，不占公共 ForkJoinPool。
        // readBounded 自终止（主进程死后宽限即返回），故即便分离子进程（reparent 到 init）
        // 仍持有管道、永不产生 EOF，主进程已产出的输出也不会丢失、读线程也不会悬挂。
        CompletableFuture<String> outputFuture = CompletableFuture.supplyAsync(
                () -> readBounded(process.getInputStream(), process, maxOutputBytes),
                r -> Thread.ofVirtual().start(r));
        boolean finished;
        try {
            finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            // impl-49：取消/中断与超时同一收口——杀整棵进程树后恢复中断标记并告知
            killProcessTree(process);
            Thread.currentThread().interrupt();
            return "run_command 已取消（进程树已终止）\n" + drainOutputQuietly(outputFuture);
        }
        if (!finished) {
            // 超时：先 best-effort 杀整棵进程树（destroyForcibly 仅杀 sh，直接子进程需 descendants 兜底）
            killProcessTree(process);
            return "run_command 超时（" + timeoutSeconds + "s），进程已终止\n"
                    + drainOutput(outputFuture);
        }
        String output = drainOutput(outputFuture);
        int exit = process.exitValue();
        return (exit == 0 ? "" : "exit=" + exit + "\n") + output;
    }

    /**
     * impl-49：环境变量白名单——子进程只看到 BASE_ENV_ALLOWLIST ∩ 父环境 + 显式追加项。
     * 此前 ProcessBuilder 默认继承父进程全部环境变量（含数据库密码/API key），机密面直接暴露给模型驱动的 shell。
     */
    private void applyEnvAllowlist(ProcessBuilder builder) {
        java.util.Map<String, String> env = builder.environment();
        java.util.Map<String, String> parent = new java.util.HashMap<>(env);
        env.clear();
        for (String name : BASE_ENV_ALLOWLIST) {
            String value = parent.get(name);
            if (value != null) {
                env.put(name, value);
            }
        }
        for (String name : extraEnvAllowlist) {
            String value = parent.get(name);
            if (value != null) {
                env.put(name, value);
            }
        }
    }

    /** 取消路径的输出回收（不抛中断叠加：中断标记已恢复，这里只尽力取已有内容）。 */
    private static String drainOutputQuietly(CompletableFuture<String> outputFuture) {
        try {
            return outputFuture.get(OUTPUT_DRAIN_GRACE_MILLIS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return "";
        }
    }

    /** 取回 readBounded 已捕获的输出。readBounded 自终止，此处仅设安全上限兜底（防异常时悬挂调用线程）。 */
    private static String drainOutput(CompletableFuture<String> outputFuture)
            throws InterruptedException {
        try {
            // 安全上限：远大于 readBounded 死后宽限（OUTPUT_DRAIN_GRACE_MILLIS）
            return outputFuture.get(OUTPUT_DRAIN_GRACE_MILLIS * 4, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return "[输出读取超时]";
        } catch (ExecutionException e) {
            return "[读取进程输出失败：" + e.getCause().getMessage() + "]";
        }
    }

    /**
     * Best-effort 杀整棵进程树。<b>已知局限</b>：分离（后台）子进程在 Linux 上会被 reparent 到 init，
     * {@code descendants()} 追踪父子关系、reparent 后丢失 → 这类孤儿清不掉、会自然到期退出。纯 Java 无
     * 可移植的 setpgid/setsid，进程组级清理留作已知边界（见类 javadoc 跨平台约束）；run_command 默认关、
     * 仅显式 opt-in 才注册，影响面限于显式开启者。
     */
    private static void killProcessTree(Process process) {
        process.toHandle().descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    /**
     * 排空进程输出，至多 maxOutputBytes（缺省 {@link #DEFAULT_MAX_OUTPUT_BYTES}）截断。
     *
     * <p><b>非阻塞轮询</b>（非 {@code readNBytes} 阻塞至 EOF）：主进程存活期间按 {@code available()}
     * 排空可得字节、{@link #POLL_MILLIS} 复检；主进程退出后再给 {@link #OUTPUT_DRAIN_GRACE_MILLIS} 宽限
     * 排空在途字节，随后即返回已捕获内容——<b>不再阻塞等待 EOF</b>。这样即使分离子进程（reparent 到
     * init）仍持有管道、永不产生 EOF，主进程已产出的输出也不会丢失、读线程也不会悬挂
     * （修前缺陷：{@code readNBytes} 阻塞至 EOF，分离子进程持有管道时丢失主进程输出）。
     */
    private static String readBounded(InputStream in, Process process, long maxOutputBytes) {
        try (in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            long diedAt = 0L;
            for (;;) {
                // 非阻塞排空当前可得字节
                while (in.available() > 0) {
                    int n = in.read(buf);
                    if (n < 0) {
                        return finish(out); // EOF：所有持有者已关闭管道，完整返回
                    }
                    out.write(buf, 0, n);
                    if (out.size() > maxOutputBytes) {
                        return truncate(out, maxOutputBytes);
                    }
                }
                if (!process.isAlive()) {
                    if (diedAt == 0L) {
                        diedAt = System.nanoTime();
                    } else if (System.nanoTime() - diedAt
                            >= TimeUnit.MILLISECONDS.toNanos(OUTPUT_DRAIN_GRACE_MILLIS)) {
                        return finish(out); // 主进程已死 + 宽限耗尽：返回已捕获，不阻塞在孤儿持有的管道
                    }
                }
                Thread.sleep(POLL_MILLIS);
            }
        } catch (IOException e) {
            return "[读取进程输出失败：" + e.getMessage() + "]";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "[读取进程输出被中断]";
        }
    }

    private static String finish(ByteArrayOutputStream out) {
        return out.toString(StandardCharsets.UTF_8);
    }

    private static String truncate(ByteArrayOutputStream out, long maxOutputBytes) {
        byte[] data = out.toByteArray();
        return new String(data, 0, (int) Math.min(data.length, maxOutputBytes), StandardCharsets.UTF_8)
                + "\n[输出超过内存兜底上限 " + maxOutputBytes + " 字节，已截断]";
    }
}
