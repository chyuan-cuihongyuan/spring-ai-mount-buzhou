package io.github.chyuan_cuihongyuan.buzhou.guard.sandbox;

import java.time.Duration;

/**
 * 沙箱资源限额（impl-40 / spec 13 §T64）：{@code run_command} 爆炸半径的输出/时间/内存上界。
 *
 * <ul>
 *   <li><b>timeout</b>：单命令执行上界（与调用方超时取更小者生效）；</li>
 *   <li><b>maxOutputBytes</b>：stdout/stderr 各自的捕获上界——超限<b>截断并显式标记</b>
 *       （{@link CommandSandbox.CommandResult#truncated()} + killedReason=OUTPUT，
 *       不静默吞也不放任内存被刷爆）；</li>
 *   <li><b>memoryBytes</b>：<b>部署侧执行器 honor</b>（如 ulimit/cgroup；JVM 无法跨平台
 *       对子进程设内存上限），被杀结果以 killedReason=MEMORY 透出。</li>
 * </ul>
 *
 * @param timeout        执行超时上界（null = 不额外限制，只用调用方超时）
 * @param maxOutputBytes 单流输出捕获上界（null 或 <=0 = 不限制）
 * @param memoryBytes    子进程内存上限（null = 不限制；部署侧执行器兑现）
 */
public record SandboxLimits(Duration timeout, Long maxOutputBytes, Long memoryBytes) {

    public static final SandboxLimits NONE = new SandboxLimits(null, null, null);

    /** 常用预设：30s 超时 + 单流 256KiB 输出上界。 */
    public static SandboxLimits defaults() {
        return new SandboxLimits(Duration.ofSeconds(30), 256 * 1024L, null);
    }
}
