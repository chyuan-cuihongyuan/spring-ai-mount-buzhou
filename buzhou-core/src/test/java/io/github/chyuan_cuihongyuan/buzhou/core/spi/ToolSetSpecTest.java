package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ToolSetSpec 直测（spec 1200 / T1801 / K 会话 R1 补测——此前零覆盖）。
 *
 * <p>断言紧凑构造器校验、env/bindings 防御性拷贝、sameConnection 差量刷新比较与
 * visibleTo 绑定可见性。
 */
class ToolSetSpecTest {

    private static ToolSetSpec spec(Map<String, String> env, Duration connect, Duration request,
                                    Set<ToolSetSpec.Binding> bindings) {
        return new ToolSetSpec("fs", Transport.STDIO, "uvx mcp-server-fs", env,
                connect, request, bindings);
    }

    @Test
    void compactConstructorRejectsBlankNameAndEndpointAndNullTransport() {
        assertThatThrownBy(() -> new ToolSetSpec(" ", Transport.STDIO, "cmd", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ToolSetSpec("fs", null, "cmd", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ToolSetSpec("fs", Transport.STDIO, "  ", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullEnvAndBindingsNormalizeToEmpty() {
        ToolSetSpec s = spec(null, null, null, null);
        assertThat(s.env()).isEmpty();
        assertThat(s.bindings()).isEmpty();
        assertThat(s.visibleTo("any-app", "any-agent")).isTrue();
    }

    @Test
    void envAndBindingsAreDefensivelyCopied() {
        Map<String, String> env = new HashMap<>();
        env.put("ROOT", "/tmp");
        Set<ToolSetSpec.Binding> bindings = new HashSet<>();
        bindings.add(new ToolSetSpec.Binding("app-1", "agent-1"));
        ToolSetSpec s = spec(env, null, null, bindings);

        env.put("ROOT", "/etc");
        bindings.add(new ToolSetSpec.Binding("app-2", "agent-2"));

        assertThat(s.env()).containsEntry("ROOT", "/tmp").hasSize(1);
        assertThat(s.bindings()).containsExactly(new ToolSetSpec.Binding("app-1", "agent-1"));
    }

    @Test
    void sameConnectionIgnoresBindingsOnly() {
        ToolSetSpec base = spec(Map.of(), null, null, Set.of());
        ToolSetSpec rebound = spec(Map.of(), null, null,
                Set.of(new ToolSetSpec.Binding("app-1", "agent-1")));
        assertThat(base.sameConnection(rebound)).isTrue();

        assertThat(base.sameConnection(spec(Map.of("K", "v"), null, null, Set.of()))).isFalse();
        assertThat(base.sameConnection(spec(Map.of(), Duration.ofSeconds(3), null, Set.of()))).isFalse();
        assertThat(base.sameConnection(spec(Map.of(), null, Duration.ofSeconds(9), Set.of()))).isFalse();
        assertThat(base.sameConnection(
                new ToolSetSpec("fs", Transport.STREAMABLE_HTTP, "https://x", Map.of(), null, null, Set.of())))
                .isFalse();
        assertThat(base.sameConnection(
                new ToolSetSpec("other", Transport.STDIO, "uvx mcp-server-fs", Map.of(), null, null, Set.of())))
                .isFalse();
        assertThat(base.sameConnection(null)).isFalse();
    }

    @Test
    void visibleToRequiresExactBindingWhenRestricted() {
        ToolSetSpec restricted = spec(Map.of(), null, null,
                Set.of(new ToolSetSpec.Binding("app-1", "agent-1")));
        assertThat(restricted.visibleTo("app-1", "agent-1")).isTrue();
        assertThat(restricted.visibleTo("app-1", "agent-2")).isFalse();
        assertThat(restricted.visibleTo("app-2", "agent-1")).isFalse();
    }
}
