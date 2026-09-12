package io.github.chyuan_cuihongyuan.buzhou.resilience.budget;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouTokenBudgetProperties;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.AtomicStateCounters;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.metadata.Usage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * agent 级成本归集 Hook（spec 65 §A / T281 / effort#25，LiteLLM spend tracking 借鉴）：
 * afterModel 把 usage（prompt/completion tokens + 定价 microUsd）按 <b>agentName</b>
 * 累计进合成会话 {@link #LEDGER_SESSION} 的 state 键——跨会话 rollup（budget 是
 * per-session；本面补「某 agent 总消耗」层）。只记账不拦截（硬顶仍归 TokenBudgetHook）。
 *
 * <p><b>挂载</b>：宿主显式构造挂载（不自动装配零新键——隐藏写放大不可接受；不挂载
 * 零行为零写）。累计经 {@link AtomicStateCounters}（spec 62）——多实例共享 store 下
 * 原子；agent 名净化入键（{@code [^A-Za-z0-9._-] → _}，与 Redis 键同纪律）。
 *
 * <p><b>诚实边界</b>：agentName 粒度（appId 不在 HookContext——fog 留位）；台账重置 =
 * 删合成会话键（运维面既有能力）；无价目模型 microUsd 记 0（tokens 仍归集）。
 *
 * @since 1.0.0
 */
public class AgentCostLedgerHook implements BuzhouHook {

    /** 台账合成会话（不进会话生命周期清理；fsck 白名单约定同源）。 */
    public static final String LEDGER_SESSION = "__buzhou.cost__";

    private static final String AGENT_PREFIX = "agent.";
    private static final String DIM_PROMPT = "prompt-tokens";
    private static final String DIM_COMPLETION = "completion-tokens";
    private static final String DIM_MICRO_USD = "cost-micro-usd";

    private final BuzhouTokenBudgetProperties pricingProps;
    private final String defaultModelName;
    private final SessionStateStore store;
    /** 台账直写句柄（绑定合成会话——HookEnvironment 门面绑定业务会话故不可复用）。 */
    private final SessionStateHandle ledgerHandle = new SessionStateHandle() {
        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> get(String key, Class<T> type) {
            return store.get(LEDGER_SESSION, key).map(StateEntry::value)
                    .filter(type::isInstance).map(v -> (T) v);
        }

        @Override
        public void put(String key, Object value) {
            store.put(LEDGER_SESSION, new StateEntry(key, String.valueOf(value),
                    "cost-ledger", 0, null, Instant.now()));
        }

        @Override
        public void delete(String key) {
            store.delete(LEDGER_SESSION, key);
        }

        /** 透传 store CAS（覆写接口非原子默认——台账原子性依赖 store 覆写实现）。 */
        @Override
        public boolean compareAndSwap(String key, String expectedValue, Object update) {
            return store.compareAndSwap(LEDGER_SESSION, key, expectedValue,
                    new StateEntry(key, String.valueOf(update), "cost-ledger", 0, null, Instant.now()));
        }
    };

    public AgentCostLedgerHook(BuzhouTokenBudgetProperties pricingProps, String defaultModelName,
            SessionStateStore store) {
        this.pricingProps = pricingProps;
        this.defaultModelName = defaultModelName == null || defaultModelName.isBlank()
                ? "unknown" : defaultModelName;
        this.store = store;
    }

    @Override
    public int order() {
        return 1150; // budget(1100) 之后：台账在预算裁决之后记账
    }

    @Override
    public HookResult afterModel(ModelCallContext ctx) {
        ChatClientResponse response = ctx.response();
        if (response == null || response.chatResponse() == null
                || response.chatResponse().getMetadata() == null) {
            return HookResult.CONTINUE;
        }
        Usage usage = response.chatResponse().getMetadata().getUsage();
        if (usage == null) {
            return HookResult.CONTINUE;
        }
        long prompt = nz(usage.getPromptTokens());
        long completion = nz(usage.getCompletionTokens());
        if (prompt == 0 && completion == 0) {
            return HookResult.CONTINUE;
        }
        String model = response.chatResponse().getMetadata().getModel() == null
                ? defaultModelName : response.chatResponse().getMetadata().getModel();
        accumulate(agentKey(ctx.agentName(), DIM_PROMPT), prompt);
        accumulate(agentKey(ctx.agentName(), DIM_COMPLETION), completion);
        accumulate(agentKey(ctx.agentName(), DIM_MICRO_USD), microUsd(model, prompt, completion));
        return HookResult.CONTINUE;
    }

    // ---- 查询面 ----

    /** per-agent 台账行。 */
    public record AgentCostRow(String agentName, long promptTokens, long completionTokens,
                               long microUsd) {
    }

    /** 扫描台账（按 agent 前缀解析；空台账 = 空表）。 */
    public static List<AgentCostRow> query(SessionStateStore store) {
        Map<String, StateEntry> entries = store.scanByPrefix(LEDGER_SESSION, AGENT_PREFIX);
        Map<String, long[]> byAgent = new LinkedHashMap<>();
        for (Map.Entry<String, StateEntry> e : entries.entrySet()) {
            String key = e.getKey().substring(AGENT_PREFIX.length());
            int lastDot = key.lastIndexOf('.');
            if (lastDot <= 0) {
                continue;
            }
            String agent = key.substring(0, lastDot);
            String dim = key.substring(lastDot + 1);
            long value = parseLong(e.getValue().value());
            long[] row = byAgent.computeIfAbsent(agent, k -> new long[3]);
            switch (dim) {
                case DIM_PROMPT -> row[0] = value;
                case DIM_COMPLETION -> row[1] = value;
                case DIM_MICRO_USD -> row[2] = value;
                default -> { /* 未知维度（前向兼容）：忽略 */ }
            }
        }
        List<AgentCostRow> rows = new ArrayList<>();
        byAgent.forEach((agent, v) -> rows.add(new AgentCostRow(agent, v[0], v[1], v[2])));
        return rows;
    }

    // ---- internals ----

    private void accumulate(String key, long delta) {
        if (delta == 0) {
            return;
        }
        AtomicStateCounters.swapValue(ledgerHandle, key,
                raw -> Long.toString(parseLong(raw) + delta), null);
    }

    private static String agentKey(String agentName, String dimension) {
        String sanitized = agentName == null || agentName.isBlank()
                ? "unknown" : agentName.replaceAll("[^A-Za-z0-9._-]", "_");
        return AGENT_PREFIX + sanitized + "." + dimension;
    }

    private long microUsd(String model, long promptTokens, long completionTokens) {
        BuzhouTokenBudgetProperties.Pricing p =
                pricingProps == null || pricingProps.pricing() == null
                        ? null : pricingProps.pricing().get(model);
        if (p == null) {
            return 0L;
        }
        long in = java.math.BigDecimal.valueOf(promptTokens).multiply(p.inputPerMillion())
                .setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
        long out = java.math.BigDecimal.valueOf(completionTokens).multiply(p.outputPerMillion())
                .setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
        return in + out;
    }

    private static long nz(Number v) {
        return v == null ? 0L : v.longValue();
    }

    private static long parseLong(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
