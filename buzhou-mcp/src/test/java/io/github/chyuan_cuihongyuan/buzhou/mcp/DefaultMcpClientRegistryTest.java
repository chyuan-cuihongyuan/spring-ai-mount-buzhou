package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicy;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicyChangeListener;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.McpServerBinding;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.PolicyConfigProvider;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Transport;
import io.github.chyuan_cuihongyuan.buzhou.mcp.internal.DefaultMcpClientRegistry;
import io.github.chyuan_cuihongyuan.buzhou.mcp.internal.McpObservability;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 差量刷新 + 引用计数关闭语义测试（ticket 15 验收项）：
 * 热更生效不重启、在途持旧连接完成后才关、超时强杀记 Error Event、
 * 未变化零重建、绑定变更不动连接。
 */
class DefaultMcpClientRegistryTest {

    private static final Duration GRACE = Duration.ofMillis(200);
    private static final Duration FORCE = Duration.ofSeconds(2);

    private final FakeMcp.Factory factory = new FakeMcp.Factory();
    private final RecordingSpanRecorder recorder = new RecordingSpanRecorder();
    private DefaultMcpClientRegistry registry;

    @AfterEach
    void tearDown() {
        if (registry != null) {
            registry.shutdown();
        }
    }

    private DefaultMcpClientRegistry newRegistry() {
        return newRegistry(GRACE, FORCE);
    }

    private DefaultMcpClientRegistry newRegistry(Duration grace, Duration force) {
        registry = new DefaultMcpClientRegistry(factory, grace, force, recorder);
        return registry;
    }

    private static ToolSetSpec spec(String name) {
        return new ToolSetSpec(name, Transport.STREAMABLE_HTTP, "http://localhost/" + name,
                Map.of(), Duration.ofSeconds(5), Duration.ofSeconds(30), Set.of());
    }

    private static ToolSetSpec specWithBindings(String name, Set<ToolSetSpec.Binding> bindings) {
        return new ToolSetSpec(name, Transport.STREAMABLE_HTTP, "http://localhost/" + name,
                Map.of(), Duration.ofSeconds(5), Duration.ofSeconds(30), bindings);
    }

    private static List<String> toolNames(List<ToolCallback> callbacks) {
        return callbacks.stream().map(c -> c.getToolDefinition().name()).toList();
    }

    @Test
    void refreshAddsAndRemovesWithoutRestart() {
        DefaultMcpClientRegistry reg = newRegistry();
        reg.refresh(List.of(spec("a"), spec("b")));

        assertThat(toolNames(reg.toolCallbacksFor("app", "agent")))
                .containsExactlyInAnyOrder("tool_a", "tool_b");

        // 摘除 a、新增 c（不重启）
        reg.refresh(List.of(spec("b"), spec("c")));

        assertThat(toolNames(reg.toolCallbacksFor("app", "agent")))
                .containsExactlyInAnyOrder("tool_b", "tool_c");
        // a 无在途 → 引用归零立即关闭
        FakeMcp.await("a closed", 2000, () -> factory.latest("a").closed());
        assertThat(factory.latest("b").closed()).isFalse();
        assertThat(factory.latest("c").closed()).isFalse();

        // 事件链：mcp.added ×3（含首轮 a/b）、mcp.removed、mcp.closed(refCountZero)、mcp.refresh span ×2
        assertThat(recorder.eventsOf(McpObservability.MCP_ADDED)).hasSize(3);
        assertThat(recorder.eventsOf(McpObservability.MCP_REMOVED))
                .singleElement().satisfies(e -> assertThat(e.payload()).containsEntry("server", "a"));
        FakeMcp.await("closed event", 2000,
                () -> !recorder.eventsOf(McpObservability.MCP_CLOSED).isEmpty());
        assertThat(recorder.eventsOf(McpObservability.MCP_CLOSED).get(0).payload())
                .containsEntry("server", "a").containsEntry("reason", "refCountZero");
        assertThat(recorder.spans()).filteredOn(s -> s.name().equals(McpObservability.SPAN_REFRESH))
                .hasSize(2);
    }

    @Test
    void unchangedEntryKeepsConnectionUntouched() {
        DefaultMcpClientRegistry reg = newRegistry();
        reg.refresh(List.of(spec("a"), spec("b")));
        List<ToolCallback> before = reg.toolCallbacksFor("app", "agent");

        // 等值清单再次刷新：连接零重建
        reg.refresh(List.of(spec("a"), spec("b")));

        assertThat(factory.connectCount("a")).isEqualTo(1);
        assertThat(factory.connectCount("b")).isEqualTo(1);
        assertThat(factory.latest("a").closed()).isFalse();
        // 回调仍指向同一批底层工具
        assertThat(toolNames(reg.toolCallbacksFor("app", "agent")))
                .containsExactlyInAnyOrderElementsOf(toolNames(before));
    }

    @Test
    void specChangeSwapsConnection() {
        DefaultMcpClientRegistry reg = newRegistry();
        reg.refresh(List.of(spec("a")));
        FakeMcp.Connection old = factory.latest("a");

        // endpoint 变更 = 删旧增新
        ToolSetSpec changed = new ToolSetSpec("a", Transport.STREAMABLE_HTTP, "http://localhost/a-v2",
                Map.of(), Duration.ofSeconds(5), Duration.ofSeconds(30), Set.of());
        reg.refresh(List.of(changed));

        assertThat(factory.connectCount("a")).isEqualTo(2);
        FakeMcp.await("old a closed", 2000, old::closed);
        assertThat(factory.latest("a")).isNotSameAs(old);
        assertThat(factory.latest("a").closed()).isFalse();
        assertThat(toolNames(reg.toolCallbacksFor("app", "agent"))).containsExactly("tool_a");
    }

    @Test
    void bindingChangeDoesNotTouchConnection() {
        DefaultMcpClientRegistry reg = newRegistry();
        reg.refresh(List.of(spec("a")));    // 空 bindings = 全局
        assertThat(toolNames(reg.toolCallbacksFor("any", "agent"))).containsExactly("tool_a");

        // 仅 bindings 变更：连接不动，可见性收窄
        reg.refresh(List.of(specWithBindings("a", Set.of(new ToolSetSpec.Binding("demo", "triage")))));

        assertThat(factory.connectCount("a")).isEqualTo(1);
        assertThat(factory.latest("a").closed()).isFalse();
        assertThat(toolNames(reg.toolCallbacksFor("demo", "triage"))).containsExactly("tool_a");
        assertThat(reg.toolCallbacksFor("other", "agent")).isEmpty();

        // bindings 再改回全局：仍不动连接
        reg.refresh(List.of(spec("a")));
        assertThat(factory.connectCount("a")).isEqualTo(1);
        assertThat(toolNames(reg.toolCallbacksFor("any", "agent"))).containsExactly("tool_a");
    }

    @Test
    void inFlightCallHoldsOldConnectionUntilComplete() throws Exception {
        DefaultMcpClientRegistry reg = newRegistry();
        reg.refresh(List.of(spec("a")));
        FakeMcp.Connection conn = factory.latest("a");
        conn.tool.blocks = true;

        ToolCallback cb = reg.toolCallbacksFor("app", "agent").get(0);
        Thread caller = Thread.startVirtualThread(() -> cb.call("{}"));
        assertThat(conn.tool.callStarted.await(2, TimeUnit.SECONDS)).isTrue();

        // 热更摘除 a：对新调用不可见，但在途持旧连接不关
        reg.refresh(List.of());
        assertThat(reg.toolCallbacksFor("app", "agent")).isEmpty();
        Thread.sleep(100);  // 确认宽限期内未关（grace=200ms 未到）
        assertThat(conn.closed()).isFalse();

        // 在途完成 → 引用归零 → 关闭（graceCompleted）
        conn.tool.release.countDown();
        caller.join(2000);
        FakeMcp.await("a closed after in-flight", 2000, conn::closed);
        FakeMcp.await("closed event", 2000,
                () -> !recorder.eventsOf(McpObservability.MCP_CLOSED).isEmpty());
        assertThat(recorder.eventsOf(McpObservability.MCP_CLOSED).get(0).payload())
                .containsEntry("reason", "graceCompleted");
    }

    @Test
    void graceExpiryClosesEvenWithInFlight() throws Exception {
        DefaultMcpClientRegistry reg = newRegistry(Duration.ofMillis(150), FORCE);
        reg.refresh(List.of(spec("a")));
        FakeMcp.Connection conn = factory.latest("a");
        conn.tool.blocks = true;

        ToolCallback cb = reg.toolCallbacksFor("app", "agent").get(0);
        Thread caller = Thread.startVirtualThread(() -> cb.call("{}"));
        assertThat(conn.tool.callStarted.await(2, TimeUnit.SECONDS)).isTrue();

        reg.refresh(List.of());
        // 宽限期到期仍有在途 → 强制关闭（graceExpired），不等在途
        FakeMcp.await("a closed on grace expiry", 3000, conn::closed);
        assertThat(caller.isAlive()).isTrue();    // 在途调用本身仍在等工具返回
        FakeMcp.await("closed event", 2000,
                () -> !recorder.eventsOf(McpObservability.MCP_CLOSED).isEmpty());
        assertThat(recorder.eventsOf(McpObservability.MCP_CLOSED).get(0).payload())
                .containsEntry("reason", "graceExpired");

        conn.tool.release.countDown();
        caller.join(2000);
    }

    @Test
    void forceCloseEmitsErrorEventWhenCloseStuck() {
        DefaultMcpClientRegistry reg = newRegistry(Duration.ofMillis(100), Duration.ofMillis(400));
        reg.refresh(List.of(spec("a")));
        FakeMcp.Connection conn = factory.latest("a");
        conn.closeBlocks = true;    // close 僵死

        reg.refresh(List.of());
        // grace 到期启动 close（阻塞）；force 到期 → mcp.forceClosed Error Event
        FakeMcp.await("forceClosed event", 3000,
                () -> !recorder.eventsOf(McpObservability.MCP_FORCE_CLOSED).isEmpty());
        assertThat(recorder.eventsOf(McpObservability.MCP_FORCE_CLOSED).get(0).payload())
                .containsEntry("server", "a").containsEntry("error", true);

        conn.closeRelease.countDown();   // 放行，避免 shutdown 等待整条兜底链
    }

    @Test
    void snapshotCallbackRejectedAfterRemoval() {
        DefaultMcpClientRegistry reg = newRegistry();
        reg.refresh(List.of(spec("a")));
        FakeMcp.Connection conn = factory.latest("a");
        ToolCallback snapshot = reg.toolCallbacksFor("app", "agent").get(0);

        reg.refresh(List.of());
        FakeMcp.await("a closed", 2000, conn::closed);

        // 摘除后旧快照上的新调用被拒（失败转文本，不执行工具）
        String result = snapshot.call("{}");
        assertThat(result).contains("已被配置热更摘除");
        assertThat(conn.tool.callCount.get()).isZero();
    }

    @Test
    void shutdownDrainsAllEntries() {
        DefaultMcpClientRegistry reg = newRegistry();
        reg.refresh(List.of(spec("a"), spec("b")));
        reg.shutdown();

        FakeMcp.await("a closed", 2000, () -> factory.latest("a").closed());
        FakeMcp.await("b closed", 2000, () -> factory.latest("b").closed());
        assertThat(reg.toolCallbacksFor("app", "agent")).isEmpty();
    }

    @Test
    void connectFailureSkipsEntryAndRecordsError() {
        FakeMcp.Factory failing = new FakeMcp.Factory() {
            @Override
            public McpConnection connect(ToolSetSpec spec) {
                if (spec.name().equals("bad")) {
                    throw new RuntimeException("connect refused");
                }
                return super.connect(spec);
            }
        };
        registry = new DefaultMcpClientRegistry(failing, GRACE, FORCE, recorder);
        registry.refresh(List.of(spec("good"), spec("bad")));

        assertThat(toolNames(registry.toolCallbacksFor("app", "agent"))).containsExactly("tool_good");
        assertThat(registry.statusOf("bad")).isNull();
        assertThat(recorder.events().stream()
                .filter(e -> e.type().equals("ERROR"))
                .map(e -> e.payload()))
                .anySatisfy(p -> {
                    assertThat(p).containsEntry("server", "bad").containsEntry("phase", "connect");
                });
    }

    @Test
    void duplicateSpecNamesRejected() {
        DefaultMcpClientRegistry reg = newRegistry();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> reg.refresh(List.of(spec("a"), spec("a"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("重复");
    }

    @Test
    void subsequentRefreshDoesNotReEmitRemovedAndEvictsClosed() {
        DefaultMcpClientRegistry reg = newRegistry();
        reg.refresh(List.of(spec("a")));
        reg.refresh(List.of());
        FakeMcp.await("a closed", 2000, () -> factory.latest("a").closed());
        FakeMcp.await("a evicted", 2000, () -> reg.statusOf("a") == null);

        // 再次刷新：摘除遗留条目不再重复记 mcp.removed
        reg.refresh(List.of());
        assertThat(recorder.eventsOf(McpObservability.MCP_REMOVED)).hasSize(1);
    }

    @Test
    void bindingLevelPolicyClipsVisibility() {
        // 绑定级清单（buzhou.mcp.bindings.demo.triage = [a]）：对全局清单再裁剪一层
        PolicyConfigProvider policy = new PolicyConfigProvider() {
            @Override
            public BindingPolicy getBindingPolicy(String appId, String agentName) {
                if (appId.equals("demo") && agentName.equals("triage")) {
                    return new BindingPolicy("demo", "triage", Map.of(), List.of(),
                            List.of(new McpServerBinding("a", "STDIO", "cmd-a", null)), 1);
                }
                return BindingPolicy.empty(appId, agentName);
            }

            @Override
            public void addChangeListener(BindingPolicyChangeListener listener) {
            }
        };
        registry = new DefaultMcpClientRegistry(factory, GRACE, FORCE, recorder, policy);
        registry.refresh(List.of(spec("a"), spec("b")));

        assertThat(toolNames(registry.toolCallbacksFor("demo", "triage"))).containsExactly("tool_a");
        assertThat(toolNames(registry.toolCallbacksFor("other", "agent")))
                .containsExactlyInAnyOrder("tool_a", "tool_b");
    }

    @Test
    void refreshSpanCarriesDiffCounts() {
        DefaultMcpClientRegistry reg = newRegistry();
        reg.refresh(List.of(spec("a"), spec("b")));
        reg.refresh(List.of(spec("b"), spec("c")));

        assertThat(recorder.spans())
                .filteredOn(s -> s.name().equals(McpObservability.SPAN_REFRESH))
                .satisfies(spans -> {
                    assertThat(spans.get(0).attributes())
                            .containsEntry("added", List.of("a", "b"));
                    assertThat(spans.get(1).attributes())
                            .containsEntry("added", List.of("c"))
                            .containsEntry("removed", List.of("a"))
                            .containsEntry("kept", List.of("b"));
                });
    }
}
