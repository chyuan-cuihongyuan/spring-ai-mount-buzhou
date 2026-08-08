package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Transport;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertiesToolSetProviderTest {

    @Test
    void parsesServersMap() {
        Map<String, Object> servers = Map.of(
                "github", Map.of(
                        "transport", "STREAMABLE_HTTP",
                        "endpoint", "https://mcp.example.com/github",
                        "connect-timeout", "10s",
                        "request-timeout", "60s",
                        "env", Map.of("Authorization", "token"),
                        "bindings", List.of(Map.of("appId", "demo", "agentName", "triage"))),
                "local", Map.of(
                        "transport", "STDIO",
                        "endpoint", "npx -y @mcp/server",
                        "env", Map.of("DEBUG", "1")));

        PropertiesToolSetProvider provider = PropertiesToolSetProvider.fromServersMap(servers);
        List<ToolSetSpec> specs = provider.currentToolSets();
        assertThat(specs).hasSize(2);

        ToolSetSpec github = specs.stream().filter(s -> s.name().equals("github")).findFirst().orElseThrow();
        assertThat(github.transport()).isEqualTo(Transport.STREAMABLE_HTTP);
        assertThat(github.endpoint()).isEqualTo("https://mcp.example.com/github");
        assertThat(github.connectTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(github.requestTimeout()).isEqualTo(Duration.ofSeconds(60));
        assertThat(github.env()).containsEntry("Authorization", "token");
        assertThat(github.bindings()).containsExactly(new ToolSetSpec.Binding("demo", "triage"));
        assertThat(github.visibleTo("demo", "triage")).isTrue();
        assertThat(github.visibleTo("other", "agent")).isFalse();

        ToolSetSpec local = specs.stream().filter(s -> s.name().equals("local")).findFirst().orElseThrow();
        assertThat(local.transport()).isEqualTo(Transport.STDIO);
        assertThat(local.connectTimeout()).isNull();
        assertThat(local.bindings()).isEmpty();
        assertThat(local.visibleTo("any", "agent")).isTrue();
    }

    @Test
    void emptyServersYieldsEmptyList() {
        assertThat(PropertiesToolSetProvider.fromServersMap(null).currentToolSets()).isEmpty();
        assertThat(PropertiesToolSetProvider.fromServersMap(Map.of()).currentToolSets()).isEmpty();
    }

    @Test
    void staticSourceNeverFiresButAcceptsListener() {
        PropertiesToolSetProvider provider = PropertiesToolSetProvider.fromServersMap(Map.of());
        provider.addChangeListener(() -> {
            throw new AssertionError("静态源不应触发变更回调");
        });
        provider.currentToolSets();
    }

    @Test
    void durationParsing() {
        assertThat(PropertiesToolSetProvider.parseDuration("30s")).isEqualTo(Duration.ofSeconds(30));
        assertThat(PropertiesToolSetProvider.parseDuration("500ms")).isEqualTo(Duration.ofMillis(500));
        assertThat(PropertiesToolSetProvider.parseDuration("5m")).isEqualTo(Duration.ofMinutes(5));
        assertThat(PropertiesToolSetProvider.parseDuration("PT1M30S")).isEqualTo(Duration.ofSeconds(90));
        assertThat(PropertiesToolSetProvider.durationVal(Map.of("k", 250), "k"))
                .isEqualTo(Duration.ofMillis(250));
        assertThat(PropertiesToolSetProvider.durationVal(Map.of(), "k")).isNull();
    }

    @Test
    void invalidSpecRejected() {
        assertThatThrownBy(() -> PropertiesToolSetProvider.fromServersMap(
                Map.of("bad", Map.of("transport", "STREAMABLE_HTTP"))))
                .isInstanceOf(IllegalArgumentException.class);   // endpoint 缺失
        assertThatThrownBy(() -> PropertiesToolSetProvider.fromServersMap(
                Map.of("bad", "not-a-map")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toolSetSpecSameConnectionIgnoresBindings() {
        ToolSetSpec a = new ToolSetSpec("s", Transport.STDIO, "cmd", Map.of("K", "v"),
                Duration.ofSeconds(1), Duration.ofSeconds(2), Set.of());
        ToolSetSpec bound = new ToolSetSpec("s", Transport.STDIO, "cmd", Map.of("K", "v"),
                Duration.ofSeconds(1), Duration.ofSeconds(2),
                Set.of(new ToolSetSpec.Binding("app", "agent")));
        ToolSetSpec changed = new ToolSetSpec("s", Transport.STDIO, "cmd2", Map.of("K", "v"),
                Duration.ofSeconds(1), Duration.ofSeconds(2), Set.of());

        assertThat(a.sameConnection(bound)).isTrue();
        assertThat(a.sameConnection(changed)).isFalse();
        assertThat(a.sameConnection(null)).isFalse();
    }
}
