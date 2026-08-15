package io.github.chyuan_cuihongyuan.buzhou.skill;

import java.util.List;
import java.util.Optional;

/**
 * Skill 注册表（spec 04）。
 *
 * <p>解析顺序：DB 动态 Skill（仅 {@code PUBLISHED}）&gt; classpath 内置。
 * 绑定语义：未显式绑定 = 全部 classpath 内置可见；存在绑定 = 该显式清单（裁剪）。
 * 两种情形下 DB 覆盖语义不变（同名 DB 取代内置出现在清单与加载结果中）。
 */
public interface SkillRegistry {

    /** 当前 (appId, agentName) 绑定下可见的清单。 */
    List<SkillMetadata> listFor(String appId, String agentName);

    /** 清单页（entries 截断后条目 + total 全量数——溢出提示用，spec 35 §B / T119）。 */
    record CatalogPage(List<SkillMetadata> entries, int total) {
    }

    /**
     * spec 35 §B / T119：带溢出计数的清单页（默认 = listFor 全量无溢出——截断实现覆写）。
     */
    default CatalogPage listForPage(String appId, String agentName) {
        List<SkillMetadata> entries = listFor(appId, agentName);
        return new CatalogPage(entries, entries.size());
    }

    /**
     * impl-71 / T96：失效清单 TTL 缓存（admin 变更后立即可见，不等 TTL）。
     * 默认 no-op（无缓存实现二进制兼容）。
     */
    default void invalidateCatalogCache() {
    }

    /** 按名加载全文；先查 DB 动态 Skill（PUBLISHED），未命中再查 classpath 内置。 */
    Optional<Skill> load(String appId, String agentName, String name);

    /** 读取 Skill 内引用资源；DB 覆盖时取 DB 资源，否则取 classpath 资源。 */
    Optional<String> loadResource(String appId, String agentName, String skillName, String relativePath);

    /**
     * name 是否在该 (appId, agentName) 的可见清单内（含解析存在性）。
     *
     * <p>{@code load_skill} 入参校验与 {@code skill://} 资源解析的绑定校验共用此判定；
     * 不受清单展示上限（catalogMaxEntries）影响——上限仅约束提示词渲染。
     */
    default boolean isVisibleFor(String appId, String agentName, String name) {
        return listFor(appId, agentName).stream().anyMatch(m -> m.name().equals(name));
    }
}
