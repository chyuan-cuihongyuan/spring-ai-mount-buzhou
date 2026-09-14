package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1510 / T2271–T2272：ConfigMaps indexed 属性数字键归一——properties/命令行源
 * 的 key[i].f=v 从 Binder 的 Map 形态（{key={0={f=v}}}）归一为 List（R9 副产出发现
 * 的通用修复：fromYml 的列表键在这些源下静默失效）。
 */
class ConfigMapsIndexedCoerceTest {

    /** ① 两项 indexed 属性 → List（数值序），元素内字段叶子归一照常。 */
    @Test
    void indexedPropertiesShouldNormalizeToList() {
        new ApplicationContextRunner()
                .withPropertyValues("probe.tools[0].name=write_file",
                        "probe.tools[0].required-state=custom",
                        "probe.tools[1].name=run_command")
                .run(ctx -> {
                    Map<String, Object> m = ConfigMaps.sub(ctx.getEnvironment(), "probe");
                    Object tools = m.get("tools");
                    assertThat(tools).isInstanceOf(List.class);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> list = (List<Map<String, Object>>) tools;
                    assertThat(list).hasSize(2);
                    assertThat(list.get(0)).containsEntry("name", "write_file")
                            .containsEntry("required-state", "custom");
                    assertThat(list.get(1)).containsEntry("name", "run_command");
                });
    }

    /** ② 跨十位排序：11 项按数值序（10 不插到 1 与 2 之间——字典序坑钉住）。 */
    @Test
    void doubleDigitIndicesShouldSortNumerically() {
        String[] props = new String[11];
        for (int i = 0; i <= 10; i++) {
            props[i] = "probe.items[" + i + "].idx=" + i;
        }
        new ApplicationContextRunner()
                .withPropertyValues(props)
                .run(ctx -> {
                    Object items = ConfigMaps.sub(ctx.getEnvironment(), "probe").get("items");
                    assertThat(items).isInstanceOf(List.class);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> list = (List<Map<String, Object>>) items;
                    assertThat(list).hasSize(11);
                    // 数值序：末位是 idx=10（字典序会把它排在 idx=1 之后、idx=2 之前）
                    assertThat(((Number) list.get(10).get("idx")).longValue()).isEqualTo(10L);
                    assertThat(((Number) list.get(1).get("idx")).longValue()).isEqualTo(1L);
                });
    }

    /** ③ 混合键 map 保持 Map 不误伤（任一非数字键）。 */
    @Test
    void mixedKeyMapShouldStayMap() {
        new ApplicationContextRunner()
                .withPropertyValues("probe.cfg.name=x", "probe.cfg.count=3")
                .run(ctx -> {
                    Object cfg = ConfigMaps.sub(ctx.getEnvironment(), "probe").get("cfg");
                    assertThat(cfg).isInstanceOf(Map.class);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) cfg;
                    assertThat(map).containsEntry("name", "x").containsEntry("count", 3L);
                });
    }
}
