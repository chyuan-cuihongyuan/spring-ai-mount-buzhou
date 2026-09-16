package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2046 / T3194：ETag 条件请求合同——强弱比较语义、If-None-Match
 * 304 判定（* / 列表 / 弱比较）、If-Match 强比较 412 防护、无头不判、
 * 畸形 fail-fast。
 */
class EntityTagMatcherTest {

    @Test
    void strongComparisonShouldRejectWeakTags() {
        assertThat(EntityTagMatcher.strongMatch("\"v1\"", "\"v1\"")).isTrue();
        assertThat(EntityTagMatcher.strongMatch("W/\"v1\"", "\"v1\"")).isFalse(); // 弱不配强
        assertThat(EntityTagMatcher.strongMatch("\"v1\"", "\"v2\"")).isFalse();
    }

    @Test
    void weakComparisonShouldIgnoreWeakPrefix() {
        assertThat(EntityTagMatcher.weakMatch("W/\"v1\"", "\"v1\"")).isTrue();  // 剥前缀等
        assertThat(EntityTagMatcher.weakMatch("W/\"v1\"", "W/\"v1\"")).isTrue();
        assertThat(EntityTagMatcher.weakMatch("\"v1\"", "\"v2\"")).isFalse();
    }

    @Test
    void ifNoneMatchShouldHitFor304() {
        assertThat(EntityTagMatcher.ifNoneMatchHit("\"v1\"", "\"v1\"")).isTrue();  // 命中→304
        assertThat(EntityTagMatcher.ifNoneMatchHit("*", "\"anything\"")).isTrue(); // 通配
        // 列表任一命中（弱比较）
        assertThat(EntityTagMatcher.ifNoneMatchHit("\"v0\", W/\"v1\"", "\"v1\"")).isTrue();
        assertThat(EntityTagMatcher.ifNoneMatchHit("\"v0\", \"v2\"", "\"v1\"")).isFalse(); // 未命中
    }

    @Test
    void ifMatchShouldUseStrongComparisonForPrecondition() {
        assertThat(EntityTagMatcher.ifMatchSatisfied("\"v1\"", "\"v1\"")).isTrue();   // 满足
        assertThat(EntityTagMatcher.ifMatchSatisfied("*", "\"any\"")).isTrue();       // 通配
        assertThat(EntityTagMatcher.ifMatchSatisfied("W/\"v1\"", "\"v1\"")).isFalse(); // 弱不满足强门→412
        assertThat(EntityTagMatcher.ifMatchSatisfied("\"v0\"", "\"v1\"")).isFalse();  // 已被改→412
    }

    @Test
    void absentHeaderShouldNeverMatch() {
        assertThat(EntityTagMatcher.ifNoneMatchHit(null, "\"v1\"")).isFalse();
        assertThat(EntityTagMatcher.ifNoneMatchHit(" ", "\"v1\"")).isFalse();
        assertThat(EntityTagMatcher.ifMatchSatisfied(null, "\"v1\"")).isFalse();
    }

    @Test
    void commaSeparatedListWithSpacesShouldParse() {
        assertThat(EntityTagMatcher.ifNoneMatchHit("  \"a\" ,  \"b\"  ", "\"b\"")).isTrue(); // 空格容忍
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> EntityTagMatcher.strongMatch(null, "\"v\""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EntityTagMatcher.weakMatch("\"v\"", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EntityTagMatcher.ifNoneMatchHit("\"v\"", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EntityTagMatcher.ifMatchSatisfied("\"v\"", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
