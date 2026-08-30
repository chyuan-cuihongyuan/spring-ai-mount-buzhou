package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 提示前缀缓存（spec 126 §A / T453，vLLM / SGLang radix prefix-cache 思想）：
 * 以「提示前缀的规范形」为键的有界 LRU——同前缀的重复请求（多轮对话的前缀
 * 稳定段、模板化系统提示）可复用宿主缓存的任意值（结构化解析结果/嵌入/渲染件）。
 *
 * <p><b>命中率即能力</b>（fog 种子⑦「前缀缓存命中率」）：requests/hits/misses/
 * evictions 四计数 + {@link #hitRate()}——前缀复用率是「提示工程是否在复读」的
 * 直接信号；miss 时的 {@code onMiss} 供宿主惰性装载（与 get/put 二选一风格）。
 *
 * <p><b>键纪律</b>：键 = 宿主规范形（同前缀必须逐字节一致——顺序/空白/模板变量
 * 展开都由宿主先规范化）经 {@link #keyOf(String)} sha256；<b>本缓存不猜测语义
 * 相似</b>（那是向量面的事）。默认 {@value #DEFAULT_MAX_ENTRIES} 条 LRU 封顶
 * （accessOrder：命中即续命），满则逐最久未用——诚实计数，不静默吞。
 *
 * <p><b>线程模型</b>：per-instance 监视器锁（缓存操作微秒级，LRU 竞争不引入
 * 分段锁复杂度——与 TokenBudgetHook per-session 锁同权衡）。
 */
public final class PromptPrefixCache<V> {

    /** 默认容量（进程内有界纪律）。 */
    public static final int DEFAULT_MAX_ENTRIES = 256;

    /** 命中率快照（四计数井读——看板/告警面）。 */
    public record Stats(long requests, long hits, long misses, long evictions) {

        /** 命中率 [0,1]；无请求时 0（诚实空值：没有请求就没有比率）。 */
        public double hitRate() {
            return requests == 0 ? 0d : (double) hits / requests;
        }
    }

    private final LinkedHashMap<String, V> entries;
    private long requests;
    private long hits;
    private long misses;
    private long evictions;

    private PromptPrefixCache(int maxEntries) {
        this.entries = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, V> eldest) {
                boolean remove = size() > maxEntries;
                if (remove) {
                    evictions++;
                }
                return remove;
            }
        };
    }

    /** 默认容量实例（{@value #DEFAULT_MAX_ENTRIES} 条）。 */
    public static <V> PromptPrefixCache<V> create() {
        return new PromptPrefixCache<>(DEFAULT_MAX_ENTRIES);
    }

    /** 自定容量实例（≥1；宿主自管 sizing）。 */
    public static <V> PromptPrefixCache<V> create(int maxEntries) {
        if (maxEntries < 1) {
            throw new IllegalArgumentException("maxEntries must be >= 1: " + maxEntries);
        }
        return new PromptPrefixCache<>(maxEntries);
    }

    /** 前缀规范形 → 缓存键（sha256 hex；null/空白 fail-fast）。 */
    public static String keyOf(String canonicalPrefix) {
        if (canonicalPrefix == null || canonicalPrefix.isBlank()) {
            throw new IllegalArgumentException("canonicalPrefix must not be blank");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(canonicalPrefix.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /** 查询（命中续命 + 计数；未命中 Optional.empty）。 */
    public synchronized Optional<V> get(String key) {
        requests++;
        V value = entries.get(key);
        if (value == null) {
            misses++;
            return Optional.empty();
        }
        hits++;
        return Optional.of(value);
    }

    /** 写入（覆盖式；null 值拒绝——空值占位会污染命中率语义）。 */
    public synchronized void put(String key, V value) {
        if (value == null) {
            throw new IllegalArgumentException("cached value must not be null");
        }
        entries.put(key, value);
    }

    /** 命中或装载（miss 时以 loader 结果入缓存并返回；loader 返回 null 视为空不缓存）。 */
    public synchronized Optional<V> getOrLoad(String key, java.util.function.Supplier<V> loader) {
        requests++;
        V value = entries.get(key);
        if (value != null) {
            hits++;
            return Optional.of(value);
        }
        misses++;
        V loaded = loader == null ? null : loader.get();
        if (loaded != null) {
            entries.put(key, loaded);
        }
        return Optional.ofNullable(loaded);
    }

    /** 显式失效（宿主知道前缀语义已变——如系统提示热替换）。 */
    public synchronized void invalidate(String key) {
        entries.remove(key);
    }

    /** 四计数快照（井读）。 */
    public synchronized Stats stats() {
        return new Stats(requests, hits, misses, evictions);
    }

    /** 在册条数（测试/健康面）。 */
    public synchronized int size() {
        return entries.size();
    }
}
