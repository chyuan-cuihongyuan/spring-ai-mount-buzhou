package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Transport;
import io.github.chyuan_cuihongyuan.buzhou.mcp.internal.DefaultMcpClientRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MCP keepalive 空闲探活测试（spec 703 / T957–T958 / impl 506）：探活真发
 * RPC（listToolNames）、失败重建（旧排空 + 新 ACTIVE）、良性邻居不误伤、
 * 已摘除条目不重复动作。
 */
class McpKeepaliveProbeTest {

    /** 可控探活的伪连接：listToolNames 计数 + 可注入失败。 */
    static final class ProbeConnection implements McpConnection {
        final String server;
        final AtomicInteger probes = new AtomicInteger();
        volatile boolean failProbe;
        final AtomicInteger closeCount = new AtomicInteger();

        ProbeConnection(String server) {
            this.server = server;
        }

        @Override
        public List<ToolCallback> toolCallbacks() {
            return List.of();
        }

        @Override
        public List<String> listToolNames() {
            probes.incrementAndGet();
            if (failProbe) {
                throw new IllegalStateException("探活炸了");
            }
            return List.of("tool_" + server);
        }

        @Override
        public void close() {
            closeCount.incrementAndGet();
        }
    }

    /** 每次建连必新建实例（记录最新连接）。 */
    static final class ProbeFactory implements McpConnectionFactory {
        final Map<String, ProbeConnection> latest = new ConcurrentHashMap<>();
        final AtomicInteger connectCount = new AtomicInteger();

        @Override
        public McpConnection connect(ToolSetSpec spec) {
            connectCount.incrementAndGet();
            ProbeConnection conn = new ProbeConnection(spec.name());
            latest.put(spec.name(), conn);
            return conn;
        }
    }

    private final ProbeFactory factory = new ProbeFactory();
    private final DefaultMcpClientRegistry registry = new DefaultMcpClientRegistry(
            factory, Duration.ofMillis(200), Duration.ofMillis(400),
            null, null, List.of(), null, null, null); // keepalive null——测试手动 probeOnce

    private static ToolSetSpec spec(String name) {
        return new ToolSetSpec(name, Transport.STREAMABLE_HTTP,
                "http://localhost/" + name, Map.of(), null, null, Set.of());
    }


    @AfterEach
    void tearDown() {
        registry.shutdown();
    }

    @Test
    void probeOncePerformsRealRpcAndCounts() {
        registry.refresh(List.of(spec("a")));
        ProbeConnection conn = factory.latest.get("a");

        registry.probeOnce();

        assertThat(conn.probes.get()).isEqualTo(2); // 建连基线 1 次（spec 18）+ 探活 1 次
        assertThat(registry.probeSuccessCount()).isEqualTo(1);
        assertThat(registry.probeFailureCount()).isZero();
    }

    @Test
    void failedProbeRebuildsConnection() {
        registry.refresh(List.of(spec("a")));
        ProbeConnection dead = factory.latest.get("a");
        dead.failProbe = true;

        registry.probeOnce();

        assertThat(registry.probeFailureCount()).isEqualTo(1);
        ProbeConnection fresh = factory.latest.get("a");
        assertThat(fresh).isNotSameAs(dead); // 工厂重建
        assertThat(fresh.closeCount.get()).isZero(); // 新连接未被关
        assertThat(registry.activeConnections()).isEqualTo(1); // 新条目 ACTIVE
        dead.failProbe = false;
        registry.probeOnce(); // 新连接健康——ok 计数
        assertThat(registry.probeSuccessCount()).isEqualTo(1);
    }

    @Test
    void healthyNeighborUnaffected() {
        registry.refresh(List.of(spec("good"), spec("bad")));
        ProbeConnection good = factory.latest.get("good");
        factory.latest.get("bad").failProbe = true;

        registry.probeOnce();

        assertThat(factory.latest.get("good")).isSameAs(good); // 不误伤
        assertThat(registry.activeConnections()).isEqualTo(2); // good 保留 + bad 重建
        factory.latest.get("bad").failProbe = false;
    }

    @Test
    void removedEntriesAreNotProbed() {
        registry.refresh(List.of(spec("a")));
        registry.refresh(List.of()); // 全量摘除
        int failuresBefore = (int) registry.probeFailureCount();

        registry.probeOnce();

        assertThat(registry.probeFailureCount()).isEqualTo(failuresBefore);
        assertThat(registry.activeConnections()).isZero();
    }
}
