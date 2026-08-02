package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LayeredPolicyTest {

    private final Map<String, Object> defaults = Map.of(
            "session", Map.of("idle-timeout", "30m", "lease", Map.of("ttl", "90s")),
            "memory", Map.of("enabled", true));
    private final Map<String, Object> yml = Map.of(
            "session", Map.of("idle-timeout", "10m"),
            "spill", Map.of("enabled", true, "spill-threshold-chars", 32000));
    private final Map<String, Object> binding = Map.of(
            "spill", Map.of("spill-threshold-chars", 16000));

    private final LayeredPolicy policy = new LayeredPolicy(defaults, yml, binding);

    @Test
    void scalarIsOverriddenByTopmostLayer() {
        assertThat(policy.get("session.idle-timeout")).isEqualTo("10m");
        assertThat(policy.get("spill.spill-threshold-chars")).isEqualTo(16000);
        assertThat(policy.get("memory.enabled")).isEqualTo(true);
    }

    @Test
    void mapsAreDeepMergedAcrossLayers() {
        Map<String, Object> session = policy.getMap("session");
        assertThat(session).containsEntry("idle-timeout", "10m");
        assertThat(session).containsEntry("lease", Map.of("ttl", "90s"));

        Map<String, Object> spill = policy.getMap("spill");
        assertThat(spill).containsEntry("enabled", true)
                .containsEntry("spill-threshold-chars", 16000);
    }

    @Test
    void listIsReplacedNotMerged() {
        LayeredPolicy withLists = new LayeredPolicy(
                Map.of("skills", Map.of("bound", List.of("a", "b"))),
                Map.of("skills", Map.of("bound", List.of("c"))),
                Map.of("skills", Map.of("bound", List.of("d", "e"))));
        assertThat(withLists.get("skills.bound")).isEqualTo(List.of("d", "e"));
    }

    @Test
    void missingKeyReturnsNullAndMissingMapIsEmpty() {
        assertThat(policy.get("no.such.key")).isNull();
        assertThat(policy.getMap("no.such")).isEmpty();
    }
}
