package io.github.chyuan_cuihongyuan.buzhou.starter;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1083 / impl 835：J 系读面统一契约冒烟——15 个静态 stats() 读面反射驱动
 * 三性质：组件全非负、resetForTest 后全归零、重复调用稳定。
 * 清单显式维护：新增读面须入清单（登记纪律落点）。
 */
class ReadoutContractSmokeTest {

    /** J 系 R46–R77 带 stats()+resetForTest() 统一形状的读面清单（显式维护，17 成员）。
     * R40 TodoTool（actionStats 形状）与 R2 ToolSlowLog（reset() 形状）暂不符合同一
     * 反射契约，留后续统一形状后纳入。 */
    private static final List<Class<?>> READOUTS = List.of(
            io.github.chyuan_cuihongyuan.buzhou.tools.file.WriteFileTool.class,     // R46
            io.github.chyuan_cuihongyuan.buzhou.tools.file.ReadFileTool.class,      // R47
            io.github.chyuan_cuihongyuan.buzhou.tools.http.SsrfGuard.class,         // R48
            io.github.chyuan_cuihongyuan.buzhou.tools.http.HttpRequestTool.class,   // R49
            io.github.chyuan_cuihongyuan.buzhou.tools.command.CommandBlacklist.class, // R51
            io.github.chyuan_cuihongyuan.buzhou.tools.command.RunCommandTool.class, // R52
            io.github.chyuan_cuihongyuan.buzhou.tools.command.SandboxRunCommandTool.class, // R74
            io.github.chyuan_cuihongyuan.buzhou.spill.EvictHandleTool.class,        // R53
            io.github.chyuan_cuihongyuan.buzhou.spill.StrReplaceTool.class,         // R54
            io.github.chyuan_cuihongyuan.buzhou.spill.ReadRangeTool.class,          // R62
            io.github.chyuan_cuihongyuan.buzhou.memory.episodic.EpisodeLedger.class, // R55
            io.github.chyuan_cuihongyuan.buzhou.memory.tool.CompactNowTool.class,   // R59
            io.github.chyuan_cuihongyuan.buzhou.core.fs.FileSandbox.class,          // R45
            io.github.chyuan_cuihongyuan.buzhou.core.retention.ArchivePurgeJob.class, // R78
            io.github.chyuan_cuihongyuan.buzhou.spill.SpillCipher.class,            // R79
            io.github.chyuan_cuihongyuan.buzhou.skill.manage.SkillAdminApi.class,   // R85
            io.github.chyuan_cuihongyuan.buzhou.spill.SpillService.class            // R101
    );

    private static long[] longComponents(Object stats) throws Exception {
        List<Long> values = new java.util.ArrayList<>();
        for (java.lang.reflect.RecordComponent rc : stats.getClass().getRecordComponents()) {
            if (rc.getType() == long.class) {
                values.add((Long) rc.getAccessor().invoke(stats));
            }
        }
        return values.stream().mapToLong(Long::longValue).toArray();
    }

    @Test
    void everyReadoutNonNegativeResetsToZeroAndIsStable() throws Exception {
        assertThat(READOUTS).allSatisfy(c -> assertThat(c.getEnclosingClass()).isNull());
        for (Class<?> readout : READOUTS) {
            Method stats = readout.getMethod("stats");
            Method reset = readout.getMethod("resetForTest");

            reset.invoke(null);
            long[] afterReset = longComponents(stats.invoke(null));
            assertThat(afterReset)
                    .as("%s reset 后应全零", readout.getSimpleName())
                    .containsOnly(0L);

            long[] first = longComponents(stats.invoke(null));
            long[] second = longComponents(stats.invoke(null));
            assertThat(java.util.Arrays.stream(first).allMatch(v -> v >= 0L))
                    .as("%s 组件应全非负", readout.getSimpleName())
                    .isTrue();
            assertThat(second).as("%s 重复调用稳定", readout.getSimpleName())
                    .isEqualTo(first);
        }
    }

    @Test
    void todoActionStatsSmoke() {
        // R40 TodoTool（actionStats Map 形状）独立冒烟：动作计数非负
        var tool = new io.github.chyuan_cuihongyuan.buzhou.tools.todo.TodoTool(
                new io.github.chyuan_cuihongyuan.buzhou.tools.todo.TodoStore(
                        new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore()));
        var stats = tool.actionStats();
        assertThat(stats.byAction().values())
                .allSatisfy(v -> assertThat(v).isGreaterThanOrEqualTo(0L));
    }

    @Test
    void toolSlowLogSmoke() {
        // R2 ToolSlowLog（全静态形状）独立冒烟：entries() 可调且返回列表
        var entries = io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolSlowLog.entries();
        assertThat(entries).isNotNull();
    }
}

