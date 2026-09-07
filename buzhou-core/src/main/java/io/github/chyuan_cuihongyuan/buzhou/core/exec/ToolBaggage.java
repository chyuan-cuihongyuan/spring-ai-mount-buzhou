package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具上下文行李（spec 337 / T665，W3C Baggage / OTel baggage 借鉴）：
 * per-runtime 有界键值面——tenant/env/region/关联单号等<b>路由元数据</b>
 * 经 ToolContext 带外直达工具，不进提示词（模型不可见不可篡改、零 token）。
 *
 * <p><b>作用域</b>：一套行李属一个 runtime（多租户宿主多 runtime 天然
 * 隔离）；yml 静态播种（buzhou.tools.baggage.*）+ 运行时 put/remove 不
 * 重启。注入工具的是 {@link #view()} 不可变快照——工具读到调用时刻的
 * 一致视图。防御面：键非空、至多 {@value #MAX_ENTRIES} 键、值至多
 * {@value #MAX_VALUE_CHARS} 字符——超限拒绝并计数（可观测不静默）。
 */
public final class ToolBaggage {

    /** ToolContext 注入键。 */
    public static final String KEY = "buzhou.baggage";

    public static final int MAX_ENTRIES = 64;
    public static final int MAX_VALUE_CHARS = 256;

    private final ConcurrentHashMap<String, String> entries = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicLong rejectedPuts =
            new java.util.concurrent.atomic.AtomicLong();

    /** 播种静态条目（装配期；yml 声明面——逐条过防御面）。 */
    public ToolBaggage(Map<String, String> seed) {
        if (seed != null) {
            seed.forEach(this::put);
        }
    }

    public ToolBaggage() {
    }

    /** 放/改一条（空键、超长值、达键数上限——拒绝并计数）。 */
    public void put(String key, String value) {
        if (key == null || key.isBlank()) {
            rejectedPuts.incrementAndGet();
            throw new IllegalArgumentException("baggage 键非空");
        }
        String val = value == null ? "" : value;
        if (val.length() > MAX_VALUE_CHARS) {
            rejectedPuts.incrementAndGet();
            throw new IllegalArgumentException(
                    "baggage 值超长（" + val.length() + ">" + MAX_VALUE_CHARS + "，key=" + key + "）");
        }
        if (entries.size() >= MAX_ENTRIES && !entries.containsKey(key)) {
            rejectedPuts.incrementAndGet();
            throw new IllegalStateException(
                    "baggage 键数达上限 " + MAX_ENTRIES + "——remove 后再 put");
        }
        entries.put(key, val);
    }

    /** 删一条（不存在 no-op）。 */
    public void remove(String key) {
        if (key != null) {
            entries.remove(key);
        }
    }

    /** 清空。 */
    public void clear() {
        entries.clear();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** 条目数（观测面）。 */
    public int size() {
        return entries.size();
    }

    /** 被拒 put 计数（观测面——防御面触发不静默）。 */
    public long rejectedPuts() {
        return rejectedPuts.get();
    }

    /** 不可变快照（注入 ToolContext 的形态）。 */
    public Map<String, String> view() {
        return Map.copyOf(entries);
    }
}
