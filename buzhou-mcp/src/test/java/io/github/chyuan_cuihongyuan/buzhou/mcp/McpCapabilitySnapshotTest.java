package io.github.chyuan_cuihongyuan.buzhou.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 822 / T1146：能力协商快照回归——名册排序+hint 计数/指纹确定性/
 * toolNames 空时降级 callbacks/seam 异常降级/双路皆空空真。
 */
class McpCapabilitySnapshotTest {

    private static ToolCallback callback(String name) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description("d")
                        .inputSchema("{\"type\":\"object\"}").build();
            }

            @Override
            public String call(String toolInput) {
                return "ok";
            }
        };
    }

    private static McpConnection connection(List<String> names, Map<String, McpToolHints> hints,
                                            List<ToolCallback> callbacks, boolean explode) {
        return new McpConnection() {
            @Override
            public List<ToolCallback> toolCallbacks() {
                if (explode) {
                    throw new IllegalStateException("seam down");
                }
                return callbacks;
            }

            @Override
            public List<String> listToolNames() {
                if (explode) {
                    throw new IllegalStateException("seam down");
                }
                return names;
            }

            @Override
            public Map<String, McpToolHints> toolHints() {
                if (explode) {
                    throw new IllegalStateException("seam down");
                }
                return hints;
            }

            @Override
            public void close() {
            }
        };
    }

    @Test
    void snapshotsSortedNamesAndHintCounts() {
        McpConnection conn = connection(List.of("z_tool", "a_tool"),
                Map.of("z_tool", new McpToolHints("Z", true, false, false, false),
                        "a_tool", new McpToolHints("A", false, true, false, false)),
                List.of(), false);

        McpCapabilitySnapshot.Snapshot snap = McpCapabilitySnapshot.of("srv", conn, 42L);
        assertThat(snap.server()).isEqualTo("srv");
        assertThat(snap.atMillis()).isEqualTo(42L);
        assertThat(snap.toolCount()).isEqualTo(2);
        assertThat(snap.toolNames()).containsExactly("a_tool", "z_tool"); // 排序
        assertThat(snap.hintCount()).isEqualTo(2);
        assertThat(snap.readOnlyCount()).isEqualTo(1);
        assertThat(snap.destructiveCount()).isEqualTo(1);
        assertThat(snap.fingerprint()).isEqualTo("a_tool,z_tool");
    }

    @Test
    void fingerprintIsDeterministic() {
        assertThat(McpCapabilitySnapshot.fingerprint(List.of("b", "a", "c")))
                .isEqualTo("a,b,c");
        assertThat(McpCapabilitySnapshot.fingerprint(List.of()))
                .isEmpty();
        // 同名册同指纹、异名册异指纹
        assertThat(McpCapabilitySnapshot.fingerprint(List.of("a", "b")))
                .isNotEqualTo(McpCapabilitySnapshot.fingerprint(List.of("a")));
    }

    @Test
    void fallsBackToCallbacksWhenNamesEmpty() {
        McpConnection conn = connection(List.of(),
                Map.of(),
                List.of(callback("cb_b"), callback("cb_a")), false);
        McpCapabilitySnapshot.Snapshot snap = McpCapabilitySnapshot.of("srv", conn, 1L);
        assertThat(snap.toolNames()).containsExactly("cb_a", "cb_b");
        assertThat(snap.toolCount()).isEqualTo(2);
    }

    @Test
    void seamExplosionsDegradeToEmpty() {
        McpConnection conn = connection(List.of(), Map.of(), List.of(), true);
        McpCapabilitySnapshot.Snapshot snap = McpCapabilitySnapshot.of("srv", conn, 1L);
        assertThat(snap.toolCount()).isZero();
        assertThat(snap.fingerprint()).isEmpty();
        assertThat(snap.hintCount()).isZero();
    }

    @Test
    void nullServerNormalizedAndNullConnectionFails() {
        McpConnection conn = connection(List.of("t"), Map.of(), List.of(), false);
        assertThat(McpCapabilitySnapshot.of(null, conn, 1L).server()).isEmpty();
        assertThatThrownBy(() -> McpCapabilitySnapshot.of("srv", null, 1L))
                .isInstanceOf(NullPointerException.class);
    }
}
