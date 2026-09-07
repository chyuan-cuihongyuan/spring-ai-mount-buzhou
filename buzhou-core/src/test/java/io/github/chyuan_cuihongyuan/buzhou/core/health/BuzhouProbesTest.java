package io.github.chyuan_cuihongyuan.buzhou.core.health;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 332 / impl-355：三探针分层回归——缺省全 readiness / 点名归类 /
 * DOWN 只连累本类 / UNKNOWN 不连累 / 空类 UP / 归类冲突红 / 幽灵机制红。
 */
class BuzhouProbesTest {

    private static final class StubHealth implements BuzhouHealth {
        private final String mechanism;
        private final Status status;

        StubHealth(String mechanism, Status status) {
            this.mechanism = mechanism;
            this.status = status;
        }

        @Override
        public String mechanism() {
            return mechanism;
        }

        @Override
        public Status status() {
            return status;
        }
    }

    private static Map<String, BuzhouHealth.Status> statusesOf(BuzhouHealth... healths) {
        java.util.LinkedHashMap<String, BuzhouHealth.Status> map = new java.util.LinkedHashMap<>();
        for (BuzhouHealth health : healths) {
            map.put(health.mechanism(), health.status());
        }
        return map;
    }

    @Test
    void unclassifiedMechanismsDefaultToReadiness() {
        BuzhouProbes probes = new BuzhouProbes(List.of(), List.of());
        Map<BuzhouProbes.ProbeClass, BuzhouProbes.Verdict> verdicts =
                probes.verdicts(statusesOf(new StubHealth("store", BuzhouHealth.Status.DOWN)));
        assertThat(verdicts.get(BuzhouProbes.ProbeClass.READINESS).status())
                .isEqualTo(BuzhouHealth.Status.DOWN);
        assertThat(verdicts.get(BuzhouProbes.ProbeClass.LIVENESS).status())
                .isEqualTo(BuzhouHealth.Status.UP); // 空类 UP
        assertThat(verdicts.get(BuzhouProbes.ProbeClass.STARTUP).status())
                .isEqualTo(BuzhouHealth.Status.UP);
    }

    @Test
    void downOnlyFailsItsOwnClass() {
        BuzhouProbes probes = new BuzhouProbes(List.of("leak"), List.of("warmup"));
        Map<BuzhouProbes.ProbeClass, BuzhouProbes.Verdict> verdicts = probes.verdicts(
                statusesOf(new StubHealth("leak", BuzhouHealth.Status.DOWN),
                        new StubHealth("warmup", BuzhouHealth.Status.UP),
                        new StubHealth("store", BuzhouHealth.Status.UP)));
        assertThat(verdicts.get(BuzhouProbes.ProbeClass.LIVENESS).status())
                .isEqualTo(BuzhouHealth.Status.DOWN);
        assertThat(verdicts.get(BuzhouProbes.ProbeClass.LIVENESS).failing())
                .containsExactly("leak");
        assertThat(verdicts.get(BuzhouProbes.ProbeClass.READINESS).status())
                .isEqualTo(BuzhouHealth.Status.UP); // 他类 DOWN 不连累
        assertThat(verdicts.get(BuzhouProbes.ProbeClass.STARTUP).status())
                .isEqualTo(BuzhouHealth.Status.UP);
    }

    @Test
    void unknownDoesNotFailAnyVerdict() {
        BuzhouProbes probes = new BuzhouProbes(List.of("leak"), List.of());
        Map<BuzhouProbes.ProbeClass, BuzhouProbes.Verdict> verdicts = probes.verdicts(
                statusesOf(new StubHealth("leak", BuzhouHealth.Status.UNKNOWN),
                        new StubHealth("store", BuzhouHealth.Status.UNKNOWN)));
        assertThat(verdicts.get(BuzhouProbes.ProbeClass.LIVENESS).status())
                .isEqualTo(BuzhouHealth.Status.UP); // 未启用 ≠ 失能
        assertThat(verdicts.get(BuzhouProbes.ProbeClass.READINESS).status())
                .isEqualTo(BuzhouHealth.Status.UP);
    }

    @Test
    void namedMechanismAppearsInItsClassMembers() {
        BuzhouProbes probes = new BuzhouProbes(List.of(), List.of("warmup"));
        Map<BuzhouProbes.ProbeClass, BuzhouProbes.Verdict> verdicts = probes.verdicts(
                statusesOf(new StubHealth("warmup", BuzhouHealth.Status.UP)));
        assertThat(verdicts.get(BuzhouProbes.ProbeClass.STARTUP).mechanisms())
                .containsExactly("warmup");
        assertThat(probes.classOf("anything-else")).isEqualTo(BuzhouProbes.ProbeClass.READINESS);
    }

    @Test
    void overlappingClassificationRejected() {
        assertThatThrownBy(() -> new BuzhouProbes(List.of("leak"), List.of("leak")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("互斥");
    }

    @Test
    void ghostMechanismFailsFastOnValidate() {
        BuzhouProbes probes = new BuzhouProbes(List.of("ghost"), List.of());
        assertThatThrownBy(() -> probes.validateMechanisms(List.of("store", "leak")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ghost");
    }

    @Test
    void blankMechanismNameRejected() {
        assertThatThrownBy(() -> new BuzhouProbes(List.of(" "), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptyWorldAllUp() {
        BuzhouProbes probes = new BuzhouProbes(null, null);
        Map<BuzhouProbes.ProbeClass, BuzhouProbes.Verdict> verdicts = probes.verdicts(Map.of());
        verdicts.values().forEach(verdict ->
                assertThat(verdict.status()).isEqualTo(BuzhouHealth.Status.UP));
    }
}
