package io.github.chyuan_cuihongyuan.buzhou.memory.summary;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1063 / impl 815：双时序事实台账操作读面——废止写入、历史/时点两类查询、
 * 损坏段装载蒸发显形（catch 按段级全损计数）、resetForTest 归零。
 */
class FactLedgerStatsTest {

    private final SessionStateStore stateStore = new InMemorySessionStateStore();

    @BeforeEach
    void reset() {
        BiTemporalFactLedger.resetForTest();
    }

    @Test
    void supersededWriteAndLookupsCountTheirBuckets() {
        BiTemporalFactLedger ledger = new BiTemporalFactLedger(stateStore);
        ledger.recordSuperseded("s1", "goals", "旧目标正文", 1, 2);
        assertThat(ledger.historyOf("s1", "goals")).isNotEmpty();
        assertThat(ledger.validAt("s1", "goals", Instant.now())).isPresent();

        BiTemporalFactLedger.FactLedgerStats stats = BiTemporalFactLedger.stats();
        assertThat(stats.supersededWrites()).isEqualTo(1);
        assertThat(stats.historyLookups()).isEqualTo(1);
        assertThat(stats.validAtLookups()).isEqualTo(1);
        assertThat(stats.corruptRecordLoads()).isZero();
    }

    @Test
    void corruptSectionLoadCountsEvictionBucket() {
        BiTemporalFactLedger ledger = new BiTemporalFactLedger(stateStore);
        // 手工注入坏 JSON 状态值（模拟存储层损坏/半截写）
        stateStore.put("s1", new StateEntry("bitemp.summary.goals", "{not-json",
                "manual", 0, null, Instant.now()));
        assertThat(ledger.historyOf("s1", "goals")).isEmpty();

        BiTemporalFactLedger.FactLedgerStats stats = BiTemporalFactLedger.stats();
        assertThat(stats.corruptRecordLoads()).isEqualTo(1);
        assertThat(stats.historyLookups()).isEqualTo(1);
    }

    @Test
    void countersAreIndependentPerOperationKind() {
        BiTemporalFactLedger ledger = new BiTemporalFactLedger(stateStore);
        ledger.recordSuperseded("s1", "facts", "正文", 1, 2);
        ledger.recordSuperseded("s1", "decisions", "正文", 1, 2);
        ledger.historyOf("s1", "facts");
        ledger.validAt("s1", "facts", Instant.now());
        ledger.validAt("s1", "decisions", Instant.now());

        BiTemporalFactLedger.FactLedgerStats stats = BiTemporalFactLedger.stats();
        // 三类操作语义不同：独立计数，无人为统一守恒（口径见 spec 1063）
        assertThat(stats.supersededWrites()).isEqualTo(2);
        assertThat(stats.historyLookups()).isEqualTo(1);
        assertThat(stats.validAtLookups()).isEqualTo(2);
    }

    @Test
    void resetForTestZeroesCounters() {
        BiTemporalFactLedger ledger = new BiTemporalFactLedger(stateStore);
        ledger.recordSuperseded("s1", "goals", "正文", 1, 2);
        assertThat(BiTemporalFactLedger.stats().supersededWrites()).isEqualTo(1);

        BiTemporalFactLedger.resetForTest();

        BiTemporalFactLedger.FactLedgerStats stats = BiTemporalFactLedger.stats();
        assertThat(stats.supersededWrites()).isZero();
        assertThat(stats.historyLookups()).isZero();
        assertThat(stats.validAtLookups()).isZero();
        assertThat(stats.corruptRecordLoads()).isZero();
    }
}
