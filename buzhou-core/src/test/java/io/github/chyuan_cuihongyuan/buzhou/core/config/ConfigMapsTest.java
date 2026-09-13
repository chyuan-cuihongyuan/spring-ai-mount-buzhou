package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ConfigMaps 直测（spec 1200 / T1801 / K 会话 R1 补测——此前零覆盖）。
 *
 * <p>走真 {@link StandardEnvironment} + {@link MapPropertySource}（全 String 来源，
 * 复现 .properties 场景），断言叶子归一化与来源一致性；嵌套 Map/List 递归归一化。
 */
class ConfigMapsTest {

    private static final String PREFIX = "buzhou.test.cfg";

    private static StandardEnvironment envWith(Map<String, Object> source) {
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("test", source));
        return env;
    }

    @Test
    void stringLeavesAreCoercedToNativeTypes() {
        StandardEnvironment env = envWith(Map.of(
                PREFIX + ".flag", "true",
                PREFIX + ".flagUpper", "FALSE",
                PREFIX + ".count", "42",
                PREFIX + ".padded", " 7 ",
                PREFIX + ".ratio", "1.5",
                PREFIX + ".name", "abc"));
        Map<String, Object> sub = ConfigMaps.sub(env, PREFIX);
        assertThat(sub.get("flag")).isEqualTo(Boolean.TRUE);
        assertThat(sub.get("flagUpper")).isEqualTo(Boolean.FALSE);
        assertThat(sub.get("count")).isEqualTo(42L);
        assertThat(sub.get("padded")).isEqualTo(7L);
        assertThat(sub.get("ratio")).isEqualTo(1.5d);
        // 非数值原样保留（含原始未 trim 形态）
        assertThat(sub.get("name")).isEqualTo("abc").isInstanceOf(String.class);
    }

    @Test
    void nestedMapAndListLeavesAreNormalizedRecursively() {
        // List 以原生对象入源（YAML 列表形态），叶子为 String——断言递归归一化
        StandardEnvironment env = envWith(Map.of(
                PREFIX + ".outer.inner", "10",
                PREFIX + ".outer.on", "true",
                PREFIX + ".items", java.util.List.of("1", "2.5")));
        Map<String, Object> sub = ConfigMaps.sub(env, PREFIX);
        Object outer = sub.get("outer");
        assertThat(outer).isInstanceOf(Map.class);
        Map<?, ?> inner = (Map<?, ?>) outer;
        assertThat(inner.get("inner")).isEqualTo(10L);
        assertThat(inner.get("on")).isEqualTo(Boolean.TRUE);
        Object items = sub.get("items");
        assertThat(items).isInstanceOf(java.util.List.class);
        java.util.List<?> list = (java.util.List<?>) items;
        assertThat(list).hasSize(2);
        assertThat(list.get(0)).isEqualTo(1L);
        assertThat(list.get(1)).isEqualTo(2.5d);
    }

    @Test
    void missingPrefixYieldsEmptyMap() {
        StandardEnvironment env = envWith(Map.of("other.prefix.x", "1"));
        Map<String, Object> sub = ConfigMaps.sub(env, "no.such.prefix");
        assertThat(sub).isEmpty();
    }

    @Test
    void returnedMapIsNormalizedCopyNotEnvironmentView() {
        StandardEnvironment env = envWith(Map.of(PREFIX + ".count", "42"));
        Map<String, Object> sub = ConfigMaps.sub(env, PREFIX);
        sub.put("extra", "x");
        assertThat(ConfigMaps.sub(env, PREFIX)).doesNotContainKey("extra");
    }
}
