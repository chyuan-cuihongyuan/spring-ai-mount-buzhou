package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaintenanceGateHistoryTest {

    @Test
    void beginThenEndProducesClosedHistoryEntry() {
        MaintenanceGate gate = new MaintenanceGate();

        gate.begin("数据库迁移", Instant.parse("2030-01-01T00:00:00Z"));
        gate.end();

        List<MaintenanceGate.HistoryEntry> history = gate.history();
        assertThat(history).hasSize(1);
        assertThat(history.get(0).window().reason()).isEqualTo("数据库迁移");
        assertThat(history.get(0).beganAt()).isNotNull();
        assertThat(history.get(0).endedAt()).isNotNull();
        assertThat(history.get(0).refusals()).isZero();
    }

    @Test
    void refusalsAreCountedIntoTheClosedEntry() {
        MaintenanceGate gate = new MaintenanceGate();

        gate.begin("发布窗口", null);
        gate.noteRefused();
        gate.noteRefused();
        gate.noteRefused();
        gate.end();

        assertThat(gate.history().get(0).refusals()).isEqualTo(3);
    }

    @Test
    void refusalOutsideWindowIsHarmlessAndUntracked() {
        MaintenanceGate gate = new MaintenanceGate();

        gate.noteRefused();
        gate.begin("r", null);
        gate.end();

        assertThat(gate.history().get(0).refusals()).isZero();
    }

    @Test
    void endWithoutBeginRemainsNoOp() {
        MaintenanceGate gate = new MaintenanceGate();

        gate.end();

        assertThat(gate.history()).isEmpty();
        assertThat(gate.isActive()).isFalse();
    }

    @Test
    void historyIsNewestFirstAndBounded() {
        MaintenanceGate gate = new MaintenanceGate();

        int total = MaintenanceGate.HISTORY_CAPACITY + 4;
        for (int i = 0; i < total; i++) {
            gate.begin("w" + i, null);
            gate.end();
        }

        List<MaintenanceGate.HistoryEntry> history = gate.history();
        assertThat(history).hasSize(MaintenanceGate.HISTORY_CAPACITY);
        assertThat(history.get(0).window().reason()).isEqualTo("w" + (total - 1));
        assertThat(history.get(history.size() - 1).window().reason()).isEqualTo("w4");
    }

    @Test
    void historySnapshotIsImmutable() {
        MaintenanceGate gate = new MaintenanceGate();
        gate.begin("r", null);
        gate.end();

        List<MaintenanceGate.HistoryEntry> snapshot = gate.history();
        assertThatThrownBy(() -> snapshot.add(snapshot.get(0)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void beginStillRequiresNonBlankReason() {
        MaintenanceGate gate = new MaintenanceGate();

        assertThatThrownBy(() -> gate.begin(" ", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(gate.isActive()).isFalse();
    }
}
