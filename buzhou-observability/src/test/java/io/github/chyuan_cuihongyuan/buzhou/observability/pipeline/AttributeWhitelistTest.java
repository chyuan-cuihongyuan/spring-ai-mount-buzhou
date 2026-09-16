package io.github.chyuan_cuihongyuan.buzhou.observability.pipeline;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2045 / T3192：属性白名单合同——盘内保留盘外计数丢弃、通配全放、
 * 空表全拒、值原样传递、聚合多属性分计、畸形 fail-fast。
 */
class AttributeWhitelistTest {

    @Test
    void attributesOutsideListShouldBeDroppedAndCounted() {
        AttributeWhitelist whitelist = new AttributeWhitelist(List.of("session.id", "tool.name"));
        AttributeWhitelist.Filtered filtered = whitelist.filter(Map.of(
                "session.id", "s-1",
                "tool.name", "read_file",
                "user.email", "a@b.c",   // 盘外
                "raw.prompt", "huge"));  // 盘外
        assertThat(filtered.retained()).containsOnlyKeys("session.id", "tool.name");
        assertThat(filtered.droppedByAttribute()).containsEntry("user.email", 1L)
                .containsEntry("raw.prompt", 1L); // 丢弃不静默——分属性计数
    }

    @Test
    void allowAllShouldRetainEverythingWithZeroDrops() {
        AttributeWhitelist whitelist = AttributeWhitelist.allowAll();
        AttributeWhitelist.Filtered filtered = whitelist.filter(Map.of("anything", 1, "secret", 2));
        assertThat(filtered.retained()).hasSize(2);
        assertThat(filtered.droppedByAttribute()).isEmpty();
        assertThat(whitelist.isAllowAll()).isTrue();
    }

    @Test
    void emptyListShouldRejectEverything() {
        AttributeWhitelist whitelist = new AttributeWhitelist(List.of());
        AttributeWhitelist.Filtered filtered = whitelist.filter(Map.of("a", 1));
        assertThat(filtered.retained()).isEmpty();
        assertThat(filtered.droppedByAttribute()).containsEntry("a", 1L); // 显式全拒
    }

    @Test
    void valuesShouldPassThroughUntouched() {
        AttributeWhitelist whitelist = new AttributeWhitelist(List.of("payload"));
        Object value = new java.util.ArrayList<>(List.of(1, 2, 3));
        AttributeWhitelist.Filtered filtered = whitelist.filter(Map.of("payload", value));
        assertThat(filtered.retained().get("payload")).isSameAs(value); // 原样引用
    }

    @Test
    void repeatedFilteringShouldAccumulatePerCall() {
        AttributeWhitelist whitelist = new AttributeWhitelist(List.of("keep"));
        whitelist.filter(Map.of("noise", 1)); // 丢 noise 1 次
        AttributeWhitelist.Filtered second = whitelist.filter(Map.of("noise", 1, "keep", "v"));
        assertThat(second.droppedByAttribute()).containsEntry("noise", 1L); // 每次调用独立计数
    }

    @Test
    void allowedListSnapshotShouldBeSortedAndStable() {
        AttributeWhitelist whitelist = new AttributeWhitelist(List.of("zeta", "alpha"));
        assertThat(whitelist.allowedAttributes()).containsExactly("alpha", "zeta"); // 字典序
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> new AttributeWhitelist(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("空表=全拒");
        assertThatThrownBy(() -> new AttributeWhitelist(java.util.Arrays.asList("ok", null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AttributeWhitelist(List.of(" ")))
                .isInstanceOf(IllegalArgumentException.class);
        AttributeWhitelist whitelist = new AttributeWhitelist(List.of("a"));
        assertThatThrownBy(() -> whitelist.filter(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
