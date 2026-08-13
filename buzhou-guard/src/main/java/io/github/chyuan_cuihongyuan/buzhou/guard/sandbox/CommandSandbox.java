package io.github.chyuan_cuihongyuan.buzhou.guard.sandbox;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 命令沙箱 SPI（wayfinder2 impl-25 / T51 / docs/spec/12 §guard-23）：
 * {@code run_command} 爆炸半径的进程级隔离档位——三档全 optional 探测启用
 * （未装即明确指引，不静默回退到裸执行）。
 *
 * <ul>
 *   <li><b>DenoSandbox</b>（必做档）：deny-by-default + 精细授权（--allow-read/net/env/run），跨平台；</li>
 *   <li><b>FirecrackerSandbox</b>（重载档，接口预留）：microVM 硬件级隔离，root+Linux-only；</li>
 *   <li><b>E2BSandbox</b>（托管档，接口预留）：Firecracker 云沙箱 REST。</li>
 * </ul>
 *
 * <p>既有 FileSandbox/黑名单定位为「无沙箱依赖内联档」（层次见 spec 07）。
 */
public interface CommandSandbox {

    /** 执行结果（exit code / stdout / stderr / 是否超时）。 */
    record CommandResult(int exitCode, String stdout, String stderr, boolean timedOut) {
        public boolean success() {
            return exitCode == 0 && !timedOut;
        }
    }

    /** 沙箱档位名（观测/配置用）。 */
    String name();

    /** 本机是否可用（探测式：依赖缺失 = false，附指引见 {@link #unavailableHint()}）。 */
    boolean available();

    /** 不可用时的部署指引。 */
    default String unavailableHint() {
        return "沙箱「" + name() + "」在本机不可用；参见 docs/spec/12 §guard-23 部署前提。";
    }

    /**
     * 在沙箱内执行命令（env 经白名单透传；workDir 须在授权读路径内）。
     *
     * @throws IllegalStateException 沙箱不可用时（不静默回退裸执行）
     */
    CommandResult run(List<String> command, Map<String, String> allowedEnv, Path workDir,
                      Duration timeout);
}
