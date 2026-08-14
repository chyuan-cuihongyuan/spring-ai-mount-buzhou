package io.github.chyuan_cuihongyuan.buzhou.guard.sandbox;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Deno 轻量沙箱档（wayfinder2 impl-25 / T51 / docs/spec/12 §guard-23，事实源 deno 108,248★）：
 * deny-by-default + 精细授权——{@code --allow-read=<路径> / --allow-net=<host:port> /
 * --allow-env=<变量名>} / {@code --allow-run=<程序名>}（secret 经环境变量白名单透传）、
 * {@code --no-prompt}（未授权即抛错，非交互）。跨平台。
 *
 * <p><b>注入安全（零内层 shell）</b>：被沙箱命令以<b>独立 argv token</b>传入固定的
 * {@code deno eval} 启动脚本（常量、不含用户内容；脚本经 {@code Deno.Command} 以 argv
 * 执行用户命令）——全链路无 shell、无命令字符串拼接，token 含空格/元字符亦不可逃逸。
 * 进程执行经 {@link SandboxProcessLauncher} 由部署侧注入（guard 不含进程原语）。
 */
public final class DenoSandbox implements CommandSandbox {

    /**
     * 固定启动脚本（常量）：读取尾随 argv（{@code --} 之后的用户命令 token），以
     * {@code Deno.Command} <b>argv 形式</b>执行（无 shell）；deno≥1.40 用 Deno.Command，
     * 旧版回退 Deno.run。
     */
    static final String BOOT_SCRIPT =
            "if (typeof Deno.Command === \"function\") {"
                    + " const s = await new Deno.Command(Deno.args[0],"
                    + " {args: Deno.args.slice(1), stdout: \"inherit\", stderr: \"inherit\"}).spawn();"
                    + " Deno.exit((await s.status).code);"
                    + "} else {"
                    + " const p = Deno.run({cmd: Deno.args});"
                    + " Deno.exit((await p.status()).code);"
                    + "}";

    private final String denoBinary;
    private final List<String> allowRead;
    private final List<String> allowNet;
    private final List<String> allowEnv;
    private final List<String> allowRun;
    private final SandboxProcessLauncher launcher;
    // impl-40 / spec 13 §T64：探测缓存（deno --version 每次 run 都探测是多余开销）+ 限额透传
    private final java.time.Duration probeTtl;
    private final SandboxLimits limits;
    private volatile ProbeResult probeCache;

    private record ProbeResult(long probedAtMillis, boolean available) {
    }

    private DenoSandbox(Builder builder) {
        this.denoBinary = builder.denoBinary;
        this.allowRead = List.copyOf(builder.allowRead);
        this.allowNet = List.copyOf(builder.allowNet);
        this.allowEnv = List.copyOf(builder.allowEnv);
        this.allowRun = List.copyOf(builder.allowRun);
        this.launcher = builder.launcher;
        this.probeTtl = builder.probeTtl;
        this.limits = builder.limits;
    }

    public static Builder builder(SandboxProcessLauncher launcher) {
        return new Builder(launcher);
    }

    public static final class Builder {
        private final SandboxProcessLauncher launcher;
        private String denoBinary = "deno";
        private final List<String> allowRead = new ArrayList<>();
        private final List<String> allowNet = new ArrayList<>();
        private final List<String> allowEnv = new ArrayList<>();
        private final List<String> allowRun = new ArrayList<>();
        private java.time.Duration probeTtl = java.time.Duration.ofSeconds(60);
        private SandboxLimits limits = SandboxLimits.NONE;

        private Builder(SandboxProcessLauncher launcher) {
            this.launcher = launcher;
        }

        /** impl-40：探测缓存 TTL（默认 PT60S；{@code Duration.ZERO} = 每次实探）。 */
        public Builder probeTtl(java.time.Duration ttl) {
            this.probeTtl = ttl == null ? java.time.Duration.ZERO : ttl;
            return this;
        }

        /** impl-40 / spec 13 §T64：资源限额（超时上界 / 输出截断 / 内存透传执行器）。 */
        public Builder limits(SandboxLimits sandboxLimits) {
            this.limits = sandboxLimits;
            return this;
        }

        public Builder denoBinary(String binary) {
            this.denoBinary = binary;
            return this;
        }

        public Builder allowRead(String path) {
            allowRead.add(path);
            return this;
        }

        public Builder allowNet(String hostPort) {
            allowNet.add(hostPort);
            return this;
        }

        /** 环境变量白名单（secret 管控面：未列名的变量不透传给沙箱内进程）。 */
        public Builder allowEnv(String variableName) {
            allowEnv.add(variableName);
            return this;
        }

        public Builder allowRun(String programName) {
            allowRun.add(programName);
            return this;
        }

        public DenoSandbox build() {
            return new DenoSandbox(this);
        }

        /** impl-40：带限额装饰的档位（limits=NONE 时等价 build()）。 */
        public CommandSandbox buildLimited() {
            DenoSandbox sandbox = new DenoSandbox(this);
            return limits == null || limits.equals(SandboxLimits.NONE)
                    ? sandbox : new LimitedCommandSandbox(sandbox, limits);
        }
    }

    @Override
    public String name() {
        return "deno";
    }

    @Override
    public boolean available() {
        // 探测：deno --version 退出码 0（经注入的 launcher 执行；失败/异常 = 不可用）
        // impl-40：结果按 TTL 缓存（探测是额外进程开销；失效即重探）
        ProbeResult cached = probeCache;
        long now = System.currentTimeMillis();
        if (cached != null && probeTtl.toMillis() > 0
                && now - cached.probedAtMillis() < probeTtl.toMillis()) {
            return cached.available();
        }
        boolean available;
        try {
            available = launcher.launch(List.of(denoBinary, "--version"), Map.of(), null,
                    Duration.ofSeconds(5)).success();
        } catch (Exception e) {
            available = false;
        }
        probeCache = new ProbeResult(now, available);
        return available;
    }

    /** 立即失效探测缓存（安装 Deno 后的运维/测试入口）。 */
    public void invalidateProbeCache() {
        probeCache = null;
    }

    @Override
    public String unavailableHint() {
        return "Deno 未安装或不在 PATH（试 `brew install deno` / `curl -fsSL https://deno.land/install.sh | sh`）；"
                + "安装后重试，或改用 Firecracker/E2B 重载档（见 docs/spec/12 §guard-23）。";
    }

    /**
     * 沙箱命令构造（包内可测）：{@code deno eval --no-prompt --allow-* <固定脚本> -- <命令 token...>}。
     * 用户命令恒为<b>独立 argv</b>（-- 之后），从不进入任何字符串拼接。
     */
    List<String> sandboxCommand(List<String> command) {
        List<String> argv = new ArrayList<>();
        argv.add(denoBinary);
        argv.add("eval");
        argv.add("--no-prompt");
        if (!allowRead.isEmpty()) {
            argv.add("--allow-read=" + String.join(",", allowRead));
        }
        if (!allowNet.isEmpty()) {
            argv.add("--allow-net=" + String.join(",", allowNet));
        }
        if (!allowEnv.isEmpty()) {
            argv.add("--allow-env=" + String.join(",", allowEnv));
        }
        if (!allowRun.isEmpty()) {
            argv.add("--allow-run=" + String.join(",", allowRun));
        }
        argv.add(BOOT_SCRIPT);
        argv.add("--");
        argv.addAll(command); // argv 直传（零 shell）
        return argv;
    }

    @Override
    public CommandResult run(List<String> command, Map<String, String> allowedEnv, Path workDir,
                             Duration timeout) {
        if (!available()) {
            throw new IllegalStateException(unavailableHint());
        }
        return launcher.launch(sandboxCommand(command),
                allowedEnv == null ? Map.of() : allowedEnv, workDir, timeout);
    }
}
