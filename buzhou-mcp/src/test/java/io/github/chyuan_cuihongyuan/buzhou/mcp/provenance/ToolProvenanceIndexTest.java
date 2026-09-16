package io.github.chyuan_cuihongyuan.buzhou.mcp.provenance;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2034 / T3170：工具溯源合同——双向账、摘除孤儿显形（独供失源/
 * 共供不孤儿）、覆盖重注册、多源冲突面、畸形 fail-fast。
 */
class ToolProvenanceIndexTest {

    @Test
    void registerShouldBuildBothDirections() {
        ToolProvenanceIndex index = new ToolProvenanceIndex();
        index.register("fs-server", List.of("read_file", "write_file"));
        index.register("web-server", List.of("fetch_page"));
        assertThat(index.providersOf("read_file")).containsExactly("fs-server");
        assertThat(index.serverCount()).isEqualTo(2);
        assertThat(index.toolCount()).isEqualTo(3);
    }

    @Test
    void unregisteringSoleProviderShouldOrphanItsTools() {
        ToolProvenanceIndex index = new ToolProvenanceIndex();
        index.register("fs-server", List.of("read_file", "write_file"));
        index.register("web-server", List.of("fetch_page"));
        Set<String> orphans = index.unregister("fs-server");
        assertThat(orphans).containsExactlyInAnyOrder("read_file", "write_file"); // 独供失源
        assertThat(index.providersOf("read_file")).isEmpty(); // 无源可查
        assertThat(index.providersOf("fetch_page")).containsExactly("web-server"); // 旁源不动
        assertThat(index.toolCount()).isEqualTo(1);
    }

    @Test
    void sharedToolShouldNotOrphanWhenOneProviderLeaves() {
        ToolProvenanceIndex index = new ToolProvenanceIndex();
        index.register("s1", List.of("search"));
        index.register("s2", List.of("search")); // 共供
        assertThat(index.unregister("s1")).isEmpty(); // 共供不孤儿
        assertThat(index.providersOf("search")).containsExactly("s2");
        assertThat(index.unregister("s2")).containsExactly("search"); // 最后一个走了才孤儿
    }

    @Test
    void reRegisterShouldReplaceToolset() {
        ToolProvenanceIndex index = new ToolProvenanceIndex();
        index.register("s1", List.of("a", "b"));
        index.register("s1", List.of("b", "c")); // 重连刷新：a 失源
        assertThat(index.providersOf("a")).isEmpty();
        assertThat(index.providersOf("c")).containsExactly("s1");
        assertThat(index.toolCount()).isEqualTo(2); // b、c
        assertThat(index.serverCount()).isEqualTo(1); // 不重复注册
    }

    @Test
    void conflictingToolsShouldSurfaceMultiProvider() {
        ToolProvenanceIndex index = new ToolProvenanceIndex();
        index.register("s1", List.of("search", "solo"));
        index.register("s2", List.of("search"));
        index.register("s3", List.of("search"));
        Map<String, Integer> conflicts = index.conflictingTools();
        assertThat(conflicts).containsEntry("search", 3); // 三源同名
        assertThat(conflicts).doesNotContainKey("solo");
    }

    @Test
    void unregisteringUnknownServerShouldReturnEmpty() {
        ToolProvenanceIndex index = new ToolProvenanceIndex();
        assertThat(index.unregister("ghost")).isEmpty();
    }

    @Test
    void malformedInputsShouldFailFast() {
        ToolProvenanceIndex index = new ToolProvenanceIndex();
        assertThatThrownBy(() -> index.register(null, List.of("t")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> index.register(" ", List.of("t")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> index.register("s", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> index.register("s", java.util.Arrays.asList("t", null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> index.register("s", List.of(" ")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> index.unregister(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> index.providersOf(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
