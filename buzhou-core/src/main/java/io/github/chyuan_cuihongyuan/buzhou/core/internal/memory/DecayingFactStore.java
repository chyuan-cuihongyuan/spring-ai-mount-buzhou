package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.Fact;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.FactStore;

import java.util.List;

/**
 * 读时置信度衰减装饰器（spec 604 / T858 / impl 457，letta memory blocks 借鉴；opt-in；
 * 自 memory 模块移驻 core 供 GuardModule 装配，spec 626）：
 * {@link #activeFacts} 在被装饰者的 TTL 过滤之上再按 {@link FactDecayPolicy} 半衰过滤——
 * 陈年低置信事实停止注入（提示词预算让位给新鲜事实）。
 *
 * <p><b>只影响注入读，不影响持久化</b>：save/delete 直通被装饰者；衰减不写回
 * （下一轮按同一公式重算，幂等可逆——换策略立即生效）。
 */
public final class DecayingFactStore implements FactStore {

    private final FactStore delegate;
    private final FactDecayPolicy policy;

    public DecayingFactStore(FactStore delegate, FactDecayPolicy policy) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate 不能为空");
        }
        this.delegate = delegate;
        this.policy = policy == null ? FactDecayPolicy.defaults() : policy;
    }

    /** 生效策略（观测面）。 */
    public FactDecayPolicy policy() {
        return policy;
    }

    @Override
    public void save(String sessionId, Fact fact) {
        delegate.save(sessionId, fact);
    }

    @Override
    public List<Fact> activeFacts(String sessionId, int currentTurn) {
        return delegate.activeFacts(sessionId, currentTurn).stream()
                .filter(fact -> policy.injectable(fact.confidence(), currentTurn - fact.createdTurn()))
                .toList();
    }

    @Override
    public void delete(String sessionId, String key) {
        delegate.delete(sessionId, key);
    }
}
