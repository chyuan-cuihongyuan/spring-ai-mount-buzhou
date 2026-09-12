package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import org.springframework.ai.chat.model.ChatResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 响应缓存 miss 惊群合并器（spec 641 / T932–T933，golang x/sync/singleflight +
 * nginx {@code proxy_cache_lock} 思想）：call 路径 miss 后同 key 并发只放一路
 * （leader）打模型，其余等待者共享其 {@link ChatResponse}——首个 miss 窗口内的
 * cache stampede（eval 并行集 / 模板化批处理 / 重试风暴）收敛为一次模型调用。
 *
 * <p><b>失败不共享</b>：leader 异常时等待者各自直调（一次性降级，不再重新合并——
 * proxy_cache_lock 语义：fetch 失败等待者重新竞争；最坏退化为无合并现状，不更差）。
 *
 * <p><b>等待与 leader 同生共死</b>：无独立等待超时——模型调用超时 / deadline 兜底
 * （advisor 链序在 resilience deadline 外层，nextCall 穿入其保护；spec 641 推理）。
 *
 * <p>in-flight 条目 leader 终态即清（两参 remove 防误删新代 entry，无泄漏）；
 * {@link #coalescedWaiters()} 计数合并省下的模型调用数（成功共享口径——降级不计数）。
 */
public final class ResponseCacheCoalescer {

    private final ConcurrentHashMap<String, CompletableFuture<ChatResponse>> inFlight = new ConcurrentHashMap<>();
    private final AtomicLong coalescedWaiters = new AtomicLong();

    /**
     * 合并执行：同 key 首个调用方（leader）独占执行 {@code call}，并发等待者共享
     * 其结果（各自拿到同一 {@link ChatResponse} 引用——与缓存命中共享 store 实例
     * 同构）；leader 失败时等待者降级各自直调。
     */
    public ChatResponse coalesce(String key, Supplier<ChatResponse> call) {
        CompletableFuture<ChatResponse> mine = new CompletableFuture<>();
        CompletableFuture<ChatResponse> leader = inFlight.putIfAbsent(key, mine);
        if (leader == null) {
            try {
                ChatResponse response = call.get();
                mine.complete(response);
                return response;
            } catch (Throwable t) {
                mine.completeExceptionally(t);
                throw t;
            } finally {
                inFlight.remove(key, mine);
            }
        }
        try {
            ChatResponse shared = leader.join();
            coalescedWaiters.incrementAndGet();
            return shared;
        } catch (CompletionException leaderFailed) {
            // 失败不共享：一次性降级各自直调（不重新合并，避免失败循环）
            return call.get();
        }
    }

    /** 合并省下的模型调用数（成功共享的等待者计数；降级不计数）。 */
    public long coalescedWaiters() {
        return coalescedWaiters.get();
    }

    /** 当前在途合并键数（leader 已出发未终态——观测/测试用）。 */
    public int inFlightCount() {
        return inFlight.size();
    }
}
