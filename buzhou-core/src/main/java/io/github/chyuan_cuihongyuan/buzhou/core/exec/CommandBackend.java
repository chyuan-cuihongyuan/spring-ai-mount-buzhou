package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.nio.file.Path;

/**
 * 命令执行端口（spec 17 / T85 / impl-60）：tools 的 {@code run_command} 与 guard 沙箱实现
 * 的唯一汇合点——两者均只依赖 core，星形依赖图不动。
 *
 * <p>实现语义：自带隔离 / 环境白名单 / 超时与进程树击杀（实现方负责）；调用方（tools）负责
 * 前置黑名单与工作目录校验。<b>不可用时抛 {@link IllegalStateException}（附指引），
 * 不静默回退裸执行。</b>
 */
public interface CommandBackend {

    /** 档位名（观测/配置用，如 {@code guard-deno-limited}）。 */
    String name();

    /**
     * 执行 shell 命令。
     *
     * @param shellCommand  shell 命令行（实现方自行 {@code /bin/sh -c} 包装或等价隔离）
     * @param workDir       工作目录（调用方已校验存在）
     * @param timeoutSeconds 超时秒数（实现方可收紧、不可放宽）
     */
    CommandOutcome run(String shellCommand, Path workDir, long timeoutSeconds);

    /** 执行结果（自含，不引实现方类型）。 */
    record CommandOutcome(int exitCode, String stdout, String stderr, boolean timedOut,
                          boolean truncated) {

        public boolean success() {
            return exitCode == 0 && !timedOut;
        }
    }
}
