package io.github.chyuan_cuihongyuan.buzhou.memory.facts;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.Fact;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.FactStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 事实置信度衰减测试（spec 604 / T858–T859 / impl 457）：半衰过滤边界、衰减公式、
 * 默认事实存活口径、save/delete 直通、策略与构造校验。信封兼容往返在 core 的
 * DefaultFactStoreTest 断言（模块边界：本模块不引 core internal）。
 */
class DecayingFactStoreTest {

    /** 内存伪实现（TTL 过滤同 DefaultFactStore 口径——被装饰者职责的替身）。 */
    private static final class FakeFactStore implements FactStore {
        final Map<String, Fact> facts = new ConcurrentHashMap<>();

        @Override
        public void save(String sessionId, Fact fact) {
            facts.put(fact.key(), fact);
        }

        @Override
        public List<Fact> activeFacts(String sessionId, int currentTurn) {
            return facts.values().stream()
                    .filter(f -> currentTurn - f.createdTurn() < f.ttl())
                    .sorted(java.util.Comparator.comparingInt(Fact::createdTurn))
                    .toList();
        }

        @Override
        public void delete(String sessionId, String key) {
            facts.remove(key);
        }
    }

    /** 半衰 4 轮 / 下限 0.25：高置信存活到恰 8 轮（0.25 边界），低置信第 4 轮即滤。 */
    @Test
    void halfLifeFiltersAgedLowConfidenceFacts() {
        FakeFactStore backing = new FakeFactStore();
        DecayingFactStore store = new DecayingFactStore(backing, new FactDecayPolicy(4, 0.25));
        store.save("s", new Fact("fact.p.a", "v", "p", 0, 1000, 1.0));
        store.save("s", new Fact("fact.p.b", "v", "p", 0, 1000, 0.4));

        assertThat(store.activeFacts("s", 4)).hasSize(1);   // a:0.5 过；b:0.2 滤
        assertThat(store.activeFacts("s", 4).get(0).key()).isEqualTo("fact.p.a");
        assertThat(store.activeFacts("s", 8)).hasSize(1);   // a:0.25 恰在下限（≥ 含）
        assertThat(store.activeFacts("s", 9)).isEmpty();    // a:≈0.21 < 0.25
    }

    /** 精确口径：decayed = conf × 2^(−elapsed/halfLife)。 */
    @Test
    void decayFormulaExact() {
        FactDecayPolicy policy = new FactDecayPolicy(4, 0.1);
        assertThat(policy.decayed(1.0, 4)).isEqualTo(0.5);
        assertThat(policy.decayed(1.0, 8)).isEqualTo(0.25);
        assertThat(policy.decayed(0.8, 0)).isEqualTo(0.8);
        assertThat(policy.injectable(1.0, 12)).isTrue();    // 0.125 ≥ 0.1
        assertThat(policy.injectable(1.0, 16)).isFalse();   // 0.0625 < 0.1
    }

    /** 五参构造（confidence=1.0）+ 长半衰：默认事实不惊扰。 */
    @Test
    void defaultConfidenceFactsSurviveLongHorizon() {
        FakeFactStore backing = new FakeFactStore();
        DecayingFactStore store = new DecayingFactStore(backing, new FactDecayPolicy(50, 0.25));
        store.save("s", new Fact("fact.p.a", "v", "p", 0, 1000));

        assertThat(store.activeFacts("s", 30)).hasSize(1);  // 1×2^(−0.6)≈0.66
        assertThat(store.activeFacts("s", 200)).isEmpty();  // 2^(−4)=0.0625 < 0.25
    }

    /** save/delete 直通被装饰者（衰减只影响注入读，不写回）。 */
    @Test
    void saveAndDeletePassThrough() {
        FakeFactStore backing = new FakeFactStore();
        DecayingFactStore store = new DecayingFactStore(backing, FactDecayPolicy.defaults());
        store.save("s", new Fact("fact.p.a", "v", "p", 0, 1000));
        assertThat(backing.activeFacts("s", 0)).hasSize(1);
        store.delete("s", "fact.p.a");
        assertThat(backing.activeFacts("s", 0)).isEmpty();
    }

    /** 策略与构造校验。 */
    @Test
    void validation() {
        assertThatThrownBy(() -> new FactDecayPolicy(0, 0.25)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FactDecayPolicy(4, 1.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DecayingFactStore(null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Fact("k", "v", "p", 0, 1, 1.5)).isInstanceOf(IllegalArgumentException.class);
    }
}
