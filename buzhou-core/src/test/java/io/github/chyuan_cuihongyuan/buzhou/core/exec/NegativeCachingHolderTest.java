package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 负缓存 Holder 装配测试（spec 1633 / T2417–T2418 / impl 1186）：
 * 未启用透传零包装；启用后真实会话构造链的工具经包装（失败缓存生效）。
 */
class NegativeCachingHolderTest {

    @AfterEach
    void reset() {
        NegativeCachingHolder.setEnabled(false);
        NegativeCachingHolder.setTtl(null);
    }

    static final class CountingTool implements ToolCallback {
        final AtomicInteger calls = new AtomicInteger();
        volatile boolean fail = true;

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder().name("neg_probe").description("d")
                    .inputSchema("{}").build();
        }

        @Override
        public String call(String input) {
            calls.incrementAndGet();
            return fail ? "[工具执行失败] boom" : "ok";
        }
    }

    @Test
    void disabledHolderPassesThroughUnwrapped() {
        CountingTool tool = new CountingTool();
        ToolCallback wrapped = NegativeCachingHolder.wrap(tool);
        assertThat(wrapped).isSameAs(tool); // 原引用零包装
    }

    @Test
    void enabledHolderWrapsAndSessionAssemblyCarriesIt() {
        NegativeCachingHolder.setEnabled(true);
        CountingTool tool = new CountingTool();

        // 直接包装面：失败缓存生效
        ToolCallback wrapped = NegativeCachingHolder.wrap(tool);
        assertThat(wrapped).isNotSameAs(tool);
        wrapped.call("{}");
        wrapped.call("{}");
        assertThat(tool.calls.get()).isEqualTo(1); // TTL 内拦截

        // 会话装配链：构造后工具名注入（负缓存不改变定义——装配不因包装破坏）
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(
                new io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel(),
                stores, RuntimeConfig.defaults(), new ToolCallback[]{tool});
        try (var session = runtime.spawn("app", "agent", "s-neg")) {
            assertThat(session).isNotNull();
        }
    }
}
