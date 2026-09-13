package io.github.chyuan_cuihongyuan.buzhou.mcp;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpParseStatsTest {

    private Map<String, Object> server(String... bindingKinds) {
        var bindings = new java.util.ArrayList<>();
        for (String kind : bindingKinds) {
            bindings.add("map".equals(kind)
                    ? Map.of("appId", "app", "agentName", "agent")
                    : kind);
        }
        return Map.of(
                "transport", "STREAMABLE_HTTP",
                "endpoint", "https://mcp.example.com",
                "bindings", bindings);
    }

    @BeforeEach
    @AfterEach
    void resetReadout() {
        PropertiesToolSetProvider.resetForTest();
    }

    @Test
    void serversAndBindingsCounted() {
        PropertiesToolSetProvider.fromServersMap(Map.of(
                "github", server("map", "map"),
                "linear", server("map")));

        PropertiesToolSetProvider.PropertiesParseStats stats = PropertiesToolSetProvider.parseStats();
        assertThat(stats.servers()).isEqualTo(2);
        assertThat(stats.bindings()).isEqualTo(3);
        assertThat(stats.bindingsSkipped()).isZero();
    }

    @Test
    void nonMapBindingItemSkippedButCounted() {
        PropertiesToolSetProvider.fromServersMap(Map.of(
                "github", Map.of(
                        "transport", "STREAMABLE_HTTP",
                        "endpoint", "https://mcp.example.com",
                        "bindings", java.util.List.of(
                                Map.of("appId", "app", "agentName", "agent"),
                                "not-a-map"))));

        PropertiesToolSetProvider.PropertiesParseStats stats = PropertiesToolSetProvider.parseStats();
        assertThat(stats.bindings()).isEqualTo(1);
        assertThat(stats.bindingsSkipped()).isEqualTo(1);
    }

    @Test
    void invalidTransportStillFailsFast() {
        assertThatThrownBy(() -> PropertiesToolSetProvider.fromServersMap(Map.of(
                "bad", Map.of("transport", "CARRIER_PIGEON"))))
                .isInstanceOf(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException.class);
    }

    @Test
    void resetForTestZeroesAllCounters() {
        PropertiesToolSetProvider.fromServersMap(Map.of(
                "github", server("map")));
        PropertiesToolSetProvider.resetForTest();

        assertThat(PropertiesToolSetProvider.parseStats())
                .isEqualTo(new PropertiesToolSetProvider.PropertiesParseStats(0, 0, 0));
    }
}
