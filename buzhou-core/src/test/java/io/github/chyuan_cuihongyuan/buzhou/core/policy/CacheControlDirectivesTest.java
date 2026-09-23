package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.policy.CacheControlDirectives.Directives;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.CacheControlDirectives.Verdict;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4048 / T6098：Cache-Control 合同——解析（大小写/引号/
 * 未知记录/负值 fail-fast）+ 裁决四象限 + 共享 s-maxage 优先 +
 * 确定性。
 */
class CacheControlDirectivesTest {

    @Test
    void parseShouldBeCaseInsensitiveAndQuoteTolerant() {
        Directives directives = CacheControlDirectives.parse(
                "PUBLIC, MAX-AGE=\"3600\", immutable, no-store");
        assertThat(directives.noStore()).isTrue();
        assertThat(directives.immutable()).isTrue();
        assertThat(directives.maxAge()).hasValue(3600L);
        assertThat(directives.unknown()).containsExactly("public");
    }

    @Test
    void unknownDirectivesShouldBeIgnoredButRecorded() {
        Directives directives = CacheControlDirectives.parse("max-age=10, x-custom, field-only");
        assertThat(directives.maxAge()).hasValue(10L);
        assertThat(directives.unknown()).containsExactly("x-custom", "field-only");
    }

    @Test
    void blankHeaderShouldBeValidEmpty() {
        Directives directives = CacheControlDirectives.parse("  ");
        assertThat(directives.noStore()).isFalse();
        assertThat(directives.maxAge()).isEmpty();
        assertThat(directives.unknown()).isEmpty();
    }

    @Test
    void negativeDeltaSecondsShouldFailFast() {
        assertThatThrownBy(() -> CacheControlDirectives.parse("max-age=-1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CacheControlDirectives.parse("s-maxage=abc"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CacheControlDirectives.parse("max-age"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verdictPriorityShouldOrderNoStoreOverNoCacheOverFreshness() {
        Directives bothDirectives = CacheControlDirectives.parse("no-store, no-cache, max-age=100");
        assertThat(CacheControlDirectives.judge(bothDirectives, 0, false))
                .isEqualTo(Verdict.NOT_CACHEABLE);   // no-store 最高
        Directives noCacheOnly = CacheControlDirectives.parse("no-cache, max-age=100");
        assertThat(CacheControlDirectives.judge(noCacheOnly, 0, false))
                .isEqualTo(Verdict.MUST_REVALIDATE);   // no-cache 次之
        Directives fresh = CacheControlDirectives.parse("max-age=100");
        assertThat(CacheControlDirectives.judge(fresh, 99, false)).isEqualTo(Verdict.FRESH);
        assertThat(CacheControlDirectives.judge(fresh, 100, false)).isEqualTo(Verdict.STALE);
    }

    @Test
    void sharedCacheShouldPreferSMaxAge() {
        Directives directives = CacheControlDirectives.parse("max-age=100, s-maxage=10");
        assertThat(CacheControlDirectives.judge(directives, 50, true)).isEqualTo(Verdict.STALE);
        assertThat(CacheControlDirectives.judge(directives, 50, false)).isEqualTo(Verdict.FRESH);
        assertThat(CacheControlDirectives.judge(directives, 5, true)).isEqualTo(Verdict.FRESH);
    }

    @Test
    void sameHeaderShouldReplaySameVerdict() {
        String header = "public, max-age=60, s-maxage=30, immutable";
        Directives first = CacheControlDirectives.parse(header);
        Directives second = CacheControlDirectives.parse(header);
        assertThat(first).isEqualTo(second);
        assertThat(CacheControlDirectives.judge(first, 20, true))
                .isEqualTo(CacheControlDirectives.judge(second, 20, true));
    }
}
