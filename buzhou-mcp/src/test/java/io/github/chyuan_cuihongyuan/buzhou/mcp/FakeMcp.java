package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试连接/工厂：记录 connect 次数（按 server 名）、close 次数与时刻；
 * 工具调用与 close 均可挂 latch 模拟「在途未归」「close 僵死」。
 */
final class FakeMcp {

    private FakeMcp() {
    }

    /** 伪连接：一个工具（名为 "tool_" + server 名），call 可阻塞；close 可阻塞。 */
    static final class Connection implements McpConnection {
        final String server;
        final AtomicInteger closeCount = new AtomicInteger();
        final CountDownLatch closeEntered = new CountDownLatch(1);
        final CountDownLatch closeRelease = new CountDownLatch(1);   // 归零前 close 阻塞
        volatile boolean closeBlocks;
        final BlockingTool tool;

        Connection(String server) {
            this.server = server;
            this.tool = new BlockingTool("tool_" + server);
        }

        @Override
        public List<ToolCallback> toolCallbacks() {
            return List.of(tool);
        }

        @Override
        public void close() {
            closeEntered.countDown();
            if (closeBlocks) {
                try {
                    closeRelease.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            closeCount.incrementAndGet();
        }

        boolean closed() {
            return closeCount.get() > 0;
        }
    }

    /** 可阻塞工具：callStarted latch 标记进入，release 归零前阻塞。 */
    static final class BlockingTool implements ToolCallback {
        private final String name;
        final CountDownLatch callStarted = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger callCount = new AtomicInteger();
        volatile boolean blocks;

        BlockingTool(String name) {
            this.name = name;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder().name(name).description("fake").inputSchema("{}").build();
        }

        @Override
        public ToolMetadata getToolMetadata() {
            return ToolMetadata.builder().build();
        }

        @Override
        public String call(String toolInput) {
            return call(toolInput, new ToolContext(Map.of()));
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            callCount.incrementAndGet();
            callStarted.countDown();
            if (blocks) {
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return "ok:" + name;
        }
    }

    /** 伪工厂：按 server 名记录 connect 次数与产物连接；可继承覆盖 connect 模拟失败。 */
    static class Factory implements McpConnectionFactory {
        final Map<String, AtomicInteger> connectCounts = new ConcurrentHashMap<>();
        final Map<String, List<Connection>> connections = new ConcurrentHashMap<>();

        @Override
        public McpConnection connect(ToolSetSpec spec) {
            connectCounts.computeIfAbsent(spec.name(), k -> new AtomicInteger()).incrementAndGet();
            Connection conn = new Connection(spec.name());
            connections.computeIfAbsent(spec.name(), k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                    .add(conn);
            return conn;
        }

        int connectCount(String server) {
            AtomicInteger c = connectCounts.get(server);
            return c == null ? 0 : c.get();
        }

        Connection latest(String server) {
            List<Connection> list = connections.get(server);
            return list == null || list.isEmpty() ? null : list.get(list.size() - 1);
        }
    }

    /** 轮询等待条件成立（时限内不成立则断言失败）。 */
    static void await(String what, long timeoutMs, java.util.function.BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("interrupted awaiting " + what);
            }
        }
        throw new AssertionError("await 超时: " + what);
    }
}
