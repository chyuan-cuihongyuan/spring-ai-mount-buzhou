package io.github.chyuan_cuihongyuan.buzhou.guard.sandbox;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 沙箱进程执行器（wayfinder2 impl-25 / T51）：由<b>部署侧注入</b>——guard 模块只做
 * 沙箱策略（argv 构造 + deny-by-default 授权面），不直接持有进程执行代码
 * （ProcessBuilder/进程原语在部署侧装配，便于其叠加审计/超时/资源限制等管控）。
 */
@FunctionalInterface
public interface SandboxProcessLauncher {

    /**
     * 以 argv 形式启动进程（无 shell）。
     *
     * @param argv    完整 argv（含解释器与脚本；用户命令为独立 token）
     * @param env     白名单环境（调用方已剥离未授权变量）
     * @param workDir 工作目录（可空）
     * @param timeout 超时（超时须销毁进程并置 timedOut）
     */
    CommandSandbox.CommandResult launch(List<String> argv, Map<String, String> env,
                                        Path workDir, Duration timeout);
}
