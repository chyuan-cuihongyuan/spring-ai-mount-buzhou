package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class LayeredPolicyAttributionTest {

    private final LayeredPolicy policy = new LayeredPolicy(
            Map.of("defaults", Map.of("only", "d"),
                    "shared", Map.of("key", "from-defaults"),
                    "nested", Map.of("deep", Map.of("leaf", "from-defaults")),
                    "scalar", Map.of("leaf", "plain")),
            Map.of("yml", Map.of("only", "y"),
                    "shared", Map.of("key", "from-yml"),
                    "nested", Map.of("deep", Map.of("leaf", "from-yml"))),
            Map.of("binding", Map.of("only", "b"),
                    "shared", Map.of("key", "from-binding")));

    @Test
    void bindingWinsOverYmlOverDefaults() {
        var attribution = policy.getAttributed("shared.key");
        assertThat(attribution.layer()).isEqualTo(PolicyLayerAttribution.Layer.BINDING);
        assertThat(attribution.value()).isEqualTo("from-binding");
    }

    @Test
    void ymlWinsWhenBindingAbsent() {
        var attribution = policy.getAttributed("yml.only");
        assertThat(attribution.layer()).isEqualTo(PolicyLayerAttribution.Layer.YML);
        assertThat(attribution.value()).isEqualTo("y");
    }

    @Test
    void defaultsUsedWhenHigherLayersAbsent() {
        var attribution = policy.getAttributed("defaults.only");
        assertThat(attribution.layer()).isEqualTo(PolicyLayerAttribution.Layer.DEFAULTS);
        assertThat(attribution.value()).isEqualTo("d");
    }

    @Test
    void absentEverywhereYieldsAbsentLayerAndNullValue() {
        var attribution = policy.getAttributed("no.such.key");
        assertThat(attribution.layer()).isEqualTo(PolicyLayerAttribution.Layer.ABSENT);
        assertThat(attribution.value()).isNull();
    }

    @Test
    void nestedDottedPathAttributesWinningLayer() {
        var attribution = policy.getAttributed("nested.deep.leaf");
        assertThat(attribution.layer()).isEqualTo(PolicyLayerAttribution.Layer.YML);
    }

    @Test
    void midPathScalarYieldsAbsent() {
        var attribution = policy.getAttributed("scalar.leaf.deeper");
        assertThat(attribution.layer()).isEqualTo(PolicyLayerAttribution.Layer.ABSENT);
        assertThat(attribution.value()).isNull();
    }

    @Test
    void getAttributedValueMatchesGetForAllLayers() {
        for (String key : new String[] {"shared.key", "yml.only", "defaults.only",
                "binding.only", "no.such.key", "nested.deep.leaf"}) {
            assertThat(policy.getAttributed(key).value()).isEqualTo(policy.get(key));
        }
    }

    @Test
    void constructorRejectsInconsistentLayerValuePairs() {
        assertThatCode(() -> new PolicyLayerAttribution("k", PolicyLayerAttribution.Layer.ABSENT, "x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> new PolicyLayerAttribution("k", PolicyLayerAttribution.Layer.YML, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> new PolicyLayerAttribution("k", PolicyLayerAttribution.Layer.ABSENT, null))
                .doesNotThrowAnyException();
    }
}
