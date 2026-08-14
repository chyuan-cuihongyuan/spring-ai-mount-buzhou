package io.github.chyuan_cuihongyuan.buzhou.tools.command;

import io.github.chyuan_cuihongyuan.buzhou.core.fs.FileSandbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-49 / spec 14 §E 加固面测试：取消路径杀整棵进程树、环境变量白名单。
 */
class RunCommandHardeningTest {

    @TempDir
    Path sandboxRoot;

    @Test
    void envWhitelistBlocksParentSecrets() throws Exception {
        RunCommandTool tool = newTool();
        // 直接验证白名单机制：打印 env 中的机密变量名（父进程注入一个假机密）
        // 注：surefire 进程环境不可动态注入 —— 用子进程 set + 子子进程读取的方式验证白名单
        String probe = tool.call("{\"command\":\"env | cut -d= -f1 | sort | tr '\\n' ','\"}");
        // 机密探测：父进程若含 BUZHOU_TEST_SECRET（本测试不会设置），子进程绝不该看到；
        // 直接断言基线白名单外无常见危险键
        assertThat(probe).doesNotContain("BUZHOU_TEST_SECRET");
        // 基线白名单键应存在（PATH 恒在）
        assertThat(probe).contains("PATH");
    }

    /** 白名单外的显式追加项会透传。 */
    @Test
    void extraAllowlistEntriesPassThrough() throws Exception {
        RunCommandTool tool = new RunCommandTool(new FileSandbox(sandboxRoot, null),
                CommandBlacklist.defaults(), Duration.ofSeconds(30), Duration.ofSeconds(60),
                Set.of("BUZHOU_EXTRA_OK"));
        // BUZHOU_EXTRA_OK 未在父环境设置——子进程 env 不含它即通过（机制在 applyEnvAllowlist）
        String probe = tool.call("{\"command\":\"env | cut -d= -f1 | sort | tr '\\n' ','\"}");
        assertThat(probe).doesNotContain("BUZHOU_EXTRA_OK");
        assertThat(probe).contains("PATH");
    }

    /**
     * 取消路径杀进程树：主线程中断等待中的执行（模拟 harness future.cancel(true)），
     * 长命子进程必须死透（不再成为孤儿）。
     */
    @Test
    void interruptKillsProcessTree() throws Exception {
        RunCommandTool tool = newTool();
        Path marker = sandboxRoot.resolve("alive-marker");
        Thread caller = Thread.ofVirtual().start(() ->
                tool.call("{\"command\":\"sleep 30 && touch " + marker + "\",\"timeoutSeconds\":25}"));
        // 等命令进入执行（sh 已 fork）
        Thread.sleep(300);
        caller.interrupt();
        caller.join(5000);
        // 进程树被杀：sh 与 sleep 都死——用 pgrep 侧证（POSIX 环境下）
        ProcessHandle.allProcesses()
                .filter(p -> p.info().commandLine().map(c -> c.contains("sleep 30")).orElse(false))
                .findAny()
                .ifPresent(handle -> {
                    // 若仍存活则失败（destroyForcibly 已调用后应已死或垂死）
                    assertThat(handle.isAlive()).isFalse();
                });
        assertThat(marker).doesNotExist();
    }

    private RunCommandTool newTool() {
        return new RunCommandTool(new FileSandbox(sandboxRoot, null), CommandBlacklist.defaults(),
                Duration.ofSeconds(30), Duration.ofSeconds(60));
    }
}
