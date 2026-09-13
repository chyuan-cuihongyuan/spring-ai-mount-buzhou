package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolInFlightTest {

    @BeforeEach
    @AfterEach
    void resetReadout() {
        ToolInFlight.reset();
    }

    @Test
    void enterIncrementsCurrentAndTotal() {
        ToolInFlight.Lease lease = ToolInFlight.enter("slow_tool");

        ToolInFlight.Snapshot snapshot = ToolInFlight.snapshot();
        assertThat(snapshot.currentTotal()).isEqualTo(1);
        assertThat(snapshot.perTool().get("slow_tool").current()).isEqualTo(1);
        assertThat(snapshot.perTool().get("slow_tool").total()).isEqualTo(1);
        assertThat(snapshot.perTool().get("slow_tool").peak()).isEqualTo(1);

        lease.close();
    }

    @Test
    void closeDecrementsCurrentButKeepsPeakAndTotal() {
        ToolInFlight.Lease lease = ToolInFlight.enter("t");
        lease.close();

        ToolInFlight.Snapshot snapshot = ToolInFlight.snapshot();
        assertThat(snapshot.currentTotal()).isZero();
        assertThat(snapshot.perTool().get("t").current()).isZero();
        assertThat(snapshot.perTool().get("t").peak()).isEqualTo(1);
        assertThat(snapshot.perTool().get("t").total()).isEqualTo(1);
    }

    @Test
    void closeIsExactlyOnce() {
        ToolInFlight.Lease lease = ToolInFlight.enter("t");

        lease.close();
        lease.close();

        assertThat(ToolInFlight.snapshot().currentTotal()).isZero();
    }

    @Test
    void peakTracksMaxConcurrentPerTool() {
        ToolInFlight.Lease first = ToolInFlight.enter("t");
        ToolInFlight.Lease second = ToolInFlight.enter("t");
        ToolInFlight.Lease third = ToolInFlight.enter("t");
        first.close();
        second.close();

        ToolInFlight.Snapshot snapshot = ToolInFlight.snapshot();
        assertThat(snapshot.perTool().get("t").current()).isEqualTo(1);
        assertThat(snapshot.perTool().get("t").peak()).isEqualTo(3);

        third.close();
    }

    @Test
    void globalPeakSpansAcrossTools() {
        ToolInFlight.Lease a = ToolInFlight.enter("a");
        ToolInFlight.Lease b = ToolInFlight.enter("b");

        ToolInFlight.Snapshot snapshot = ToolInFlight.snapshot();
        assertThat(snapshot.currentTotal()).isEqualTo(2);
        assertThat(snapshot.peakTotal()).isEqualTo(2);
        assertThat(snapshot.perTool().get("a").current()).isEqualTo(1);
        assertThat(snapshot.perTool().get("b").current()).isEqualTo(1);

        a.close();
        b.close();
    }

    @Test
    void toolsAreIsolated() {
        ToolInFlight.Lease a = ToolInFlight.enter("a");

        ToolInFlight.Snapshot snapshot = ToolInFlight.snapshot();
        assertThat(snapshot.perTool().get("a").current()).isEqualTo(1);
        assertThat(snapshot.perTool().containsKey("b")).isFalse();
        assertThat(snapshot.perTool().keySet()).containsExactlyInAnyOrder("a");

        a.close();
    }

    @Test
    void snapshotIsImmutable() {
        ToolInFlight.enter("t");
        Map<String, ToolInFlight.PerTool> perTool = ToolInFlight.snapshot().perTool();

        assertThatThrownBy(() -> perTool.put("x", new ToolInFlight.PerTool(1, 1, 1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void resetClearsAllWatermarks() {
        ToolInFlight.enter("t");

        ToolInFlight.reset();

        ToolInFlight.Snapshot snapshot = ToolInFlight.snapshot();
        assertThat(snapshot.currentTotal()).isZero();
        assertThat(snapshot.peakTotal()).isZero();
        assertThat(snapshot.perTool()).isEmpty();
    }
}
