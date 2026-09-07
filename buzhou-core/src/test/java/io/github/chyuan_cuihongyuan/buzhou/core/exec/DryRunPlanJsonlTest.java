package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 328 / impl-351：干跑计划导出回归——逐行字段对账/顺序即拦截序/
 * 空计划零行/dropped 尾行/不可序列化 args 字符串降级不炸。
 */
class DryRunPlanJsonlTest {

    private static final HookEnvironment ENV =
            new HookEnvironment("s1", "agent", new InMemorySessionStateStore());

    private static DryRunHook planWith(String tool, Map<String, Object> args) {
        DryRunHook hook = new DryRunHook(Set.of(), true);
        DefaultToolCallContext ctx =
                new DefaultToolCallContext(ENV, "tc-" + tool, tool, args);
        HookResult result = hook.beforeTool(ctx);
        assertThat(result).isInstanceOf(HookResult.Block.class);
        return hook;
    }

    @Test
    void exportsRowsInInterceptionOrder() throws Exception {
        DryRunHook hook = new DryRunHook(Set.of(), true);
        hook.beforeTool(new DefaultToolCallContext(ENV, "tc-1", "search",
                Map.of("q", "天气")));
        hook.beforeTool(new DefaultToolCallContext(ENV, "tc-2", "delete_user",
                Map.of("id", "u-9")));

        StringWriter out = new StringWriter();
        long lines = DryRunPlanJsonl.export(hook, out);
        assertThat(lines).isEqualTo(2);
        String[] rows = out.toString().trim().split("\n");
        assertThat(rows).hasSize(2);
        assertThat(rows[0])
                .contains("\"toolCallId\":\"tc-1\"")
                .contains("\"tool\":\"search\"")
                .contains("天气");
        assertThat(rows[1])
                .contains("\"toolCallId\":\"tc-2\"")
                .contains("\"tool\":\"delete_user\"")
                .contains("u-9"); // 顺序即拦截序
    }

    @Test
    void emptyPlanExportsZeroLines() throws Exception {
        DryRunHook hook = new DryRunHook(Set.of(), false); // 未开零计划
        StringWriter out = new StringWriter();
        assertThat(DryRunPlanJsonl.export(hook, out)).isZero();
        assertThat(out.toString()).isEmpty(); // 空计划零行诚实（连 meta 也没有）
    }

    @Test
    void droppedCountSurfacesAsMetaTailRow() throws Exception {
        DryRunHook hook = new DryRunHook(Set.of(), true);
        for (int i = 0; i < DryRunHook.MAX_PLANNED + 5; i++) {
            hook.beforeTool(new DefaultToolCallContext(ENV, "tc-" + i,
                    "write", Map.of("i", i)));
        }
        StringWriter out = new StringWriter();
        long lines = DryRunPlanJsonl.export(hook, out);
        assertThat(lines).isEqualTo(DryRunHook.MAX_PLANNED);
        String[] rows = out.toString().trim().split("\n");
        assertThat(rows).hasSize(DryRunHook.MAX_PLANNED + 1);
        assertThat(rows[rows.length - 1])
                .as("截断诚实可见——meta 尾行")
                .contains("\"meta\":true").contains("\"dropped\":5");
    }

    @Test
    void unserializableArgsDegradeToString() throws Exception {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("callback", new Object() { // 无任何可序列化属性
            @Override public String toString() {
                return "lambda@1";
            }
        });
        DryRunHook hook = planWith("register", args);
        StringWriter out = new StringWriter();
        assertThat(DryRunPlanJsonl.export(hook, out)).isEqualTo(1);
        assertThat(out.toString())
                .as("args 字符串快照列——不可序列化值不炸导出")
                .contains("lambda@1");
    }
}
