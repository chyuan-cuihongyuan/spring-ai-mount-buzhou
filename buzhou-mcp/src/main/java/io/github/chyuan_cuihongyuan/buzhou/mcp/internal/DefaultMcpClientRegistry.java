package io.github.chyuan_cuihongyuan.buzhou.mcp.internal;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContext;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanRecorder;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.PolicyConfigProvider;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;
import io.github.chyuan_cuihongyuan.buzhou.mcp.McpClientRegistry;
import io.github.chyuan_cuihongyuan.buzhou.mcp.McpConnection;
import io.github.chyuan_cuihongyuan.buzhou.mcp.McpConnectionFactory;
import org.springframework.ai.tool.ToolCallback;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 注册表默认实现（spec 04）：差量刷新 + 引用计数延迟关闭 + 强杀兜底 + 可观测事件。
 *
 * <p>并发模型：{@link #refresh} 串行化（refreshLock）；条目状态迁移在条目锁内；
 * {@code connection.close()} 一律在独立虚拟线程执行——调度线程只触发不阻塞，
 * 故「close 僵死」不会卡死宽限期调度，强杀兜底（forceCloseTimeout）必到。
 *
 * <p>关闭时序（条目入 DRAINING 后）：
 * <ul>
 *   <li>inFlight==0 → 立即关闭，reason=refCountZero</li>
 *   <li>在途归零 → 关闭，reason=graceCompleted</li>
 *   <li>gracePeriod 到期仍有在途 → 关闭，reason=graceExpired</li>
 *   <li>forceCloseTimeout 到期仍未关完 → 独立线程强制 close + Error Event(mcp.forceClosed)</li>
 * </ul>
 */
public class DefaultMcpClientRegistry implements McpClientRegistry {

    public enum Status {ACTIVE, DRAINING, CLOSED}

    /** 注册表条目（spec 04 内部结构）。 */
    public static final class Entry {
        private final String name;
        private final McpConnection connection;
        private final AtomicInteger inFlight = new AtomicInteger(0);
        private final Object lock = new Object();
        /** bindings 变更只换 spec（连接不动），故 volatile 整体替换 */
        private volatile ToolSetSpec spec;
        private volatile Status status = Status.ACTIVE;
        private volatile Instant drainingSince;
        /** close 启动后置位；完成/异常时 complete，供强杀判定与 shutdown 等待 */
        private volatile CompletableFuture<Void> closeFuture;
        /** 本条目所属 refresh 的 span context，供异步关闭事件挂靠 */
        private volatile SpanContext spanContext;

        Entry(String name, ToolSetSpec spec, McpConnection connection) {
            this.name = name;
            this.spec = spec;
            this.connection = connection;
        }

        public String name() {
            return name;
        }

        public Status status() {
            return status;
        }

        public int inFlight() {
            return inFlight.get();
        }

        /** 进入 DRAINING 的时刻（未摘除返回 null）。 */
        public Instant drainingSince() {
            return drainingSince;
        }
    }

    private final McpConnectionFactory factory;
    private final Duration gracePeriod;
    private final Duration forceCloseTimeout;
    private final McpObservability obs;
    private final PolicyConfigProvider policyProvider;   // 可空：绑定级清单裁剪视图
    /** impl-50：客户端侧危险工具名模式（glob，如 *.delete*）；空 = 不登记。 */
    private final java.util.List<java.util.regex.Pattern> dangerousToolPatterns;
    /** impl-50：最近一次建连失败计数（健康面）。 */
    private final java.util.concurrent.atomic.AtomicLong connectFailures = new java.util.concurrent.atomic.AtomicLong();
    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();
    private final Object refreshLock = new Object();
    private final ScheduledExecutorService scheduler;
    private volatile boolean shutdown;

    /** 既有 4 参构造兼容（policyProvider=null）。 */
    public DefaultMcpClientRegistry(McpConnectionFactory factory, Duration gracePeriod,
                                    Duration forceCloseTimeout, SpanRecorder recorder) {
        this(factory, gracePeriod, forceCloseTimeout, recorder, null, java.util.List.of());
    }

    /** 既有 5 参构造兼容（危险模式空表）。 */
    public DefaultMcpClientRegistry(McpConnectionFactory factory, Duration gracePeriod,
                                    Duration forceCloseTimeout, SpanRecorder recorder,
                                    PolicyConfigProvider policyProvider) {
        this(factory, gracePeriod, forceCloseTimeout, recorder, policyProvider, java.util.List.of());
    }

    /** impl-50 全参：额外接收危险工具模式列表。 */
    public DefaultMcpClientRegistry(McpConnectionFactory factory, Duration gracePeriod,
                                    Duration forceCloseTimeout, SpanRecorder recorder,
                                    PolicyConfigProvider policyProvider,
                                    java.util.List<String> dangerousToolPatterns) {
        this.factory = factory;
        this.gracePeriod = gracePeriod;
        this.forceCloseTimeout = forceCloseTimeout;
        this.obs = new McpObservability(recorder);
        this.policyProvider = policyProvider;
        this.dangerousToolPatterns = dangerousToolPatterns == null ? java.util.List.of()
                : dangerousToolPatterns.stream().map(DefaultMcpClientRegistry::globToRegex).toList();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "buzhou-mcp-registry-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public List<ToolCallback> toolCallbacksFor(String appId, String agentName) {
        // 绑定级清单（buzhou.mcp.bindings.<appId>.<agentName> = serverName 列表）：
        // 非空时对全局清单再裁剪一层（spec 04 配置项节「绑定本质是配置」）
        Set<String> bindingClip = bindingClip(appId, agentName);
        List<ToolCallback> out = new ArrayList<>();
        for (Entry e : entries.values()) {
            if (e.status == Status.ACTIVE && e.spec.visibleTo(appId, agentName)
                    && (bindingClip == null || bindingClip.contains(e.name()))) {
                for (ToolCallback cb : e.connection.toolCallbacks()) {
                    out.add(new RefCountingToolCallback(cb, e, this));
                }
            }
        }
        return out;
    }

    /** 绑定级裁剪集；null = 无绑定级清单（不裁剪）。 */
    private Set<String> bindingClip(String appId, String agentName) {
        if (policyProvider == null) {
            return null;
        }
        var policy = policyProvider.getBindingPolicy(appId, agentName);
        if (policy == null || policy.mcpServers().isEmpty()) {
            return null;
        }
        Set<String> names = new HashSet<>();
        policy.mcpServers().forEach(b -> names.add(b.name()));
        return names;
    }

    @Override
    public void refresh(List<ToolSetSpec> newSpecs) {
        synchronized (refreshLock) {
            if (shutdown) {
                return;
            }
            Map<String, ToolSetSpec> newByName = new LinkedHashMap<>();
            for (ToolSetSpec spec : newSpecs) {
                if (newByName.put(spec.name(), spec) != null) {
                    throw new IllegalArgumentException("ToolSetSpec name 重复: " + spec.name());
                }
            }

            Map<String, Object> attrs = new LinkedHashMap<>();
            attrs.put("specs.total", newSpecs.size());
            SpanHandle span = obs.openRefreshSpan(attrs);
            SpanContext spanCtx = span == null ? null : span.context();

            List<String> added = new ArrayList<>();
            List<String> removed = new ArrayList<>();
            List<String> kept = new ArrayList<>();
            List<String> changed = new ArrayList<>();

            // 保持 / 删除 / 变更（删旧增新）；非 ACTIVE 条目是上次 refresh 的摘除遗留，跳过
            for (Entry e : entries.values()) {
                if (e.status != Status.ACTIVE) {
                    continue;
                }
                ToolSetSpec ns = newByName.get(e.name());
                if (ns == null) {
                    obs.removed(spanCtx, e.name(), null);
                    markDraining(e, spanCtx);
                    removed.add(e.name());
                } else if (e.spec.sameConnection(ns)) {
                    if (!e.spec.bindings().equals(ns.bindings())) {
                        e.spec = ns;    // 绑定变更不动连接，只更新可见性映射
                    }
                    kept.add(e.name());
                } else {
                    obs.removed(spanCtx, e.name(), "spec-changed");
                    markDraining(e, spanCtx);
                    removed.add(e.name());
                    changed.add(e.name());
                    if (addEntry(ns, spanCtx)) {
                        added.add(ns.name());
                    }
                }
            }
            // 新增
            for (ToolSetSpec spec : newByName.values()) {
                if (!entries.containsKey(spec.name())) {
                    if (addEntry(spec, spanCtx)) {
                        added.add(spec.name());
                    }
                }
            }

            if (span != null) {
                span.attribute("added", List.copyOf(added));
                span.attribute("removed", List.copyOf(removed));
                span.attribute("kept", List.copyOf(kept));
                span.attribute("changed", List.copyOf(changed));
                span.close();
            }
        }
    }

    /** 建连 + 注册 ACTIVE 条目；失败记 ERROR Event 并跳过（不影响其余条目）。 */
    private boolean addEntry(ToolSetSpec spec, SpanContext spanCtx) {
        McpConnection connection;
        try {
            connection = factory.connect(spec);
        } catch (RuntimeException e) {
            obs.connectFailed(spanCtx, spec.name(), e);
            // impl-50：建连失败计数 + 指标（运维面；此前仅 Span Event、无指标）
            connectFailures.incrementAndGet();
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                    .counter("buzhou.mcp.connect.failures", "server", spec.name());
            return false;
        }
        Entry entry = new Entry(spec.name(), spec, connection);
        entry.spanContext = spanCtx;
        entries.put(spec.name(), entry);
        obs.added(spanCtx, spec.name(), null);
        return true;
    }

    /** 置 DRAINING（对新调用即刻不可见）并启动关闭等待；inFlight==0 立即关闭。 */
    private void markDraining(Entry e, SpanContext spanCtx) {
        synchronized (e.lock) {
            if (e.status != Status.ACTIVE) {
                return;
            }
            e.status = Status.DRAINING;
            e.drainingSince = Instant.now();
            e.spanContext = spanCtx;
            // closeFuture 随 DRAINING 立即就位：shutdown 等待与强杀判定都依赖它非空
            e.closeFuture = new CompletableFuture<>();
        }
        scheduler.schedule(() -> onGraceExpired(e), gracePeriod.toMillis(), TimeUnit.MILLISECONDS);
        scheduler.schedule(() -> onForceClose(e), forceCloseTimeout.toMillis(), TimeUnit.MILLISECONDS);
        if (e.inFlight.get() == 0) {
            tryClose(e, "refCountZero");
        }
    }

    /** 引用获取（RefCountingToolCallback 入口）：仅 ACTIVE 条目接新调用。 */
    boolean tryAcquire(Entry e) {
        synchronized (e.lock) {
            if (e.status != Status.ACTIVE) {
                return false;
            }
            e.inFlight.incrementAndGet();
            return true;
        }
    }

    /** 引用释放（finally）：DRAINING 中归零 → graceCompleted 关闭。 */
    void release(Entry e) {
        boolean zero;
        synchronized (e.lock) {
            e.inFlight.decrementAndGet();
            zero = e.status == Status.DRAINING && e.inFlight.get() == 0;
        }
        if (zero) {
            tryClose(e, "graceCompleted");
        }
    }

    private void onGraceExpired(Entry e) {
        synchronized (e.lock) {
            if (e.status != Status.DRAINING) {
                return;
            }
        }
        tryClose(e, "graceExpired");
    }

    /** 强杀兜底：close 阻塞/连接僵死时独立线程强制 close + Error Event。 */
    private void onForceClose(Entry e) {
        CompletableFuture<Void> f = e.closeFuture;
        if (f == null || f.isDone()) {
            return;
        }
        obs.forceClosed(e.spanContext, e.name());
        if (e.status != Status.CLOSED) {
            // 关闭从未启动（宽限期任务未跑到等极端情形）：直接按宽限到期路径关
            tryClose(e, "graceExpired");
            return;
        }
        Thread.startVirtualThread(() -> {
            try {
                e.connection.close();
                // 原 close 线程僵死、强杀成功：补发终态 closed 事件（reason=forceClosed，推演 13）
                obs.closed(e.spanContext, e.name(), "forceClosed");
            } catch (Throwable t) {
                obs.closeFailed(e.spanContext, e.name(), t);
            } finally {
                f.complete(null);
            }
        });
    }

    /** 幂等启动关闭：置 CLOSED（同条目锁，与 tryAcquire 互斥）后虚拟线程执行物理关闭。 */
    private void tryClose(Entry e, String reason) {
        synchronized (e.lock) {
            if (e.status == Status.CLOSED) {
                return;
            }
            e.status = Status.CLOSED;
            if (e.closeFuture == null) {
                e.closeFuture = new CompletableFuture<>();
            }
        }
        CompletableFuture<Void> f = e.closeFuture;
        Thread.startVirtualThread(() -> {
            try {
                e.connection.close();
                obs.closed(e.spanContext, e.name(), reason);
                f.complete(null);
            } catch (Throwable t) {
                // close 失败：reason 闭集（refCountZero/graceCompleted/graceExpired/forceClosed）不篡改，
                // 另发 ERROR Event（phase=close）
                obs.closeFailed(e.spanContext, e.name(), t);
                f.completeExceptionally(t);
            }
            // 摘除条目关完后移出注册表（变更场景下 map 里是同名新条目，身份比较防误删）
            entries.remove(e.name(), e);
        });
    }

    @Override
    public void shutdown() {
        List<Entry> draining;
        synchronized (refreshLock) {
            if (shutdown) {
                return;
            }
            shutdown = true;
            draining = new ArrayList<>(entries.values());
            for (Entry e : draining) {
                markDraining(e, e.spanContext);
            }
        }
        // 等待全部条目归零或兜底到期（grace + force 之外再留 1s 余量让强杀线程收尾）
        long deadlineMs = System.currentTimeMillis()
                + gracePeriod.toMillis() + forceCloseTimeout.toMillis() + 1000;
        for (Entry e : draining) {
            CompletableFuture<Void> f = e.closeFuture;
            if (f == null) {
                continue;
            }
            long waitMs = Math.max(1, deadlineMs - System.currentTimeMillis());
            try {
                f.get(waitMs, TimeUnit.MILLISECONDS);
            } catch (Exception ignored) {
                // 强杀线程已兜底，shutdown 不抛
            }
        }
        scheduler.shutdownNow();
    }

    /** 测试探针：条目当前状态（无条目返回 null）。 */
    public Status statusOf(String name) {
        Entry e = entries.get(name);
        return e == null ? null : e.status;
    }

    /** 测试探针：条目在途计数（无条目返回 -1）。 */
    public int inFlightOf(String name) {
        Entry e = entries.get(name);
        return e == null ? -1 : e.inFlight();
    }

    /** impl-50：glob（*.delete*）→ 正则。 */
    static java.util.regex.Pattern globToRegex(String glob) {
        StringBuilder regex = new StringBuilder();
        for (char c : glob.toCharArray()) {
            switch (c) {
                case '*' -> regex.append(".*");
                case '?' -> regex.append('.');
                case '.' -> regex.append("\\.");
                default -> {
                    if (!Character.isLetterOrDigit(c) && !Character.isJavaIdentifierPart(c)) {
                        regex.append('\\');
                    }
                    regex.append(c);
                }
            }
        }
        return java.util.regex.Pattern.compile(regex.toString(),
                java.util.regex.Pattern.CASE_INSENSITIVE);
    }

    @Override
    public int activeConnections() {
        int count = 0;
        for (Entry e : entries.values()) {
            if (e.status == Status.ACTIVE) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int drainingConnections() {
        int count = 0;
        for (Entry e : entries.values()) {
            if (e.status == Status.DRAINING) {
                count++;
            }
        }
        return count;
    }

    @Override
    public java.util.Set<String> dangerousToolNames() {
        if (dangerousToolPatterns.isEmpty()) {
            return java.util.Set.of();
        }
        java.util.Set<String> out = new java.util.LinkedHashSet<>();
        for (Entry e : entries.values()) {
            if (e.status != Status.ACTIVE) {
                continue;
            }
            for (ToolCallback cb : e.connection.toolCallbacks()) {
                String name = cb.getToolDefinition().name();
                for (java.util.regex.Pattern pattern : dangerousToolPatterns) {
                    if (pattern.matcher(name).matches()) {
                        out.add(name);
                        break;
                    }
                }
            }
        }
        return java.util.Set.copyOf(out);
    }

    /** impl-50：建连失败计数（connectFailure 路径自增；健康面只读）。 */
    long connectFailures() {
        return connectFailures.get();
    }
}
