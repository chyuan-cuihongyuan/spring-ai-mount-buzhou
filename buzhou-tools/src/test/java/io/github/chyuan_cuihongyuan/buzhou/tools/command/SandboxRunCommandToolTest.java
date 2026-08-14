package io.github.chyuan_cuihongyuan.buzhou.tools.command;

import io.github.chyuan_cuihongyuan.buzhou.core.exec.CommandBackend;
import io.github.chyuan_cuihongyuan.buzhou.core.fs.FileSandbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * run_command 沙箱委托版单测（spec 17 / T85 / impl-60）：委托路由、前置校验防线、
 * 结果格式化、不可用不回退、ToolsModule 装配二选一与 fail-fast。
 */
class SandboxRunCommandToolTest {

    /** 记录调用并回放结果的假 backend。 */
    static final class RecordingBackend implements CommandBackend {
        final List<String> commands = new ArrayList<>();
        final List<Path> workdirs = new ArrayList<>();
        CommandOutcome outcome = new CommandOutcome(0, "done", "", false, false);

        @Override
        public String name() {
            return "fake-sandbox";
        }

        @Override
        public CommandOutcome run(String shellCommand, Path workDir, long timeoutSeconds) {
            commands.add(shellCommand);
            workdirs.add(workDir);
            return outcome;
        }
    }

    @TempDir
    Path tmp;

    /** 成功委托：命令与工作目录透传 backend，输出格式化保留 exit/stderr/truncated 标记。 */
    @Test
    void delegatesExecutionToBackend() throws Exception {
        RecordingBackend backend = new RecordingBackend();
        Path root = Files.createDirectory(tmp.resolve("sandbox"));
        SandboxRunCommandTool tool = newTool(root, backend);

        String out = tool.call("{\"command\":\"echo hi\"}");

        assertThat(backend.commands).containsExactly("echo hi");
        assertThat(backend.workdirs).containsExactly(root);
        assertThat(out).isEqualTo("done");

        backend.outcome = new CommandOutcome(3, "so-so", "warn-line", false, true);
        String out2 = tool.call("{\"command\":\"x\",\"timeoutSeconds\":5}");
        assertThat(out2).contains("exit=3").contains("so-so").contains("stderr:").contains("warn-line")
                .contains("截断").contains("fake-sandbox");
    }

    /** 前置防线保留在 tools：黑名单命中即拒（backend 零调用）。 */
    @Test
    void blacklistStillEnforcedBeforeBackend() {
        RecordingBackend backend = new RecordingBackend();
        SandboxRunCommandTool tool = newTool(tmp, backend);

        String out = tool.call("{\"command\":\"rm -rf /\"}");

        assertThat(out).contains("黑名单");
        assertThat(backend.commands).isEmpty();
    }

    /** workdir 逐段防线：`..` 与非法段拒绝；合法相对路径拼进沙箱 root。 */
    @Test
    void workdirSegmentValidation() throws Exception {
        RecordingBackend backend = new RecordingBackend();
        Path root = Files.createDirectory(tmp.resolve("sandbox"));
        Path sub = Files.createDirectory(root.resolve("sub"));
        SandboxRunCommandTool tool = newTool(root, backend);

        String ok = tool.call("{\"command\":\"pwd\",\"workdir\":\"sub\"}");
        assertThat(ok).isEqualTo("done");
        assertThat(backend.workdirs).containsExactly(sub);

        assertThat(tool.call("{\"command\":\"pwd\",\"workdir\":\"../escape\"}")).contains("失败");
        assertThat(tool.call("{\"command\":\"pwd\",\"workdir\":\"a;b\"}")).contains("失败");
        assertThat(backend.commands).hasSize(1); // 越界/非法均未触达 backend
    }

    /** 沙箱不可用不静默回退：失败文本附 backend 名与指引。 */
    @Test
    void unavailableSandboxDoesNotFallBack() {
        CommandBackend unavailable = new CommandBackend() {
            @Override
            public String name() {
                return "deno-missing";
            }

            @Override
            public CommandOutcome run(String shellCommand, Path workDir, long timeoutSeconds) {
                throw new IllegalStateException("deno binary not found; see docs");
            }
        };
        SandboxRunCommandTool tool = newTool(tmp, unavailable);

        String out = tool.call("{\"command\":\"echo hi\"}");

        assertThat(out).contains("沙箱不可用").contains("deno-missing").contains("拒绝裸执行回退");
    }

    /** ToolsModule 装配：backend=sandbox 且无实现 → fail-fast；有 backend 则注册委托版。 */
    @Test
    void toolsModuleAssemblySwitch() {
        assertThatThrownBy(() -> io.github.chyuan_cuihongyuan.buzhou.tools.ToolsModule
                .builder(null)
                .fromYml(java.util.Map.of("run-command", java.util.Map.of("enabled", true),
                        "command", java.util.Map.of("backend", "sandbox")))
                .build())
                .isInstanceOf(io.github.chyuan_cuihongyuan.buzhou.core.config
                        .BuzhouConfigurationException.class)
                .hasMessageContaining("CommandBackend");

        io.github.chyuan_cuihongyuan.buzhou.tools.ToolsModule withBackend =
                io.github.chyuan_cuihongyuan.buzhou.tools.ToolsModule
                        .builder(null)
                        .fromYml(java.util.Map.of("run-command", java.util.Map.of("enabled", true)))
                        .commandBackend(new RecordingBackend())
                        .build();
        assertThat(withBackend.configure().autoTools())
                .anyMatch(t -> t.getToolDefinition().name().equals("run_command"));
    }

    private SandboxRunCommandTool newTool(Path root, CommandBackend backend) {
        return new SandboxRunCommandTool(new FileSandbox(root, java.util.List.of()),
                null, backend, Duration.ofSeconds(60), Duration.ofMinutes(10));
    }
}
