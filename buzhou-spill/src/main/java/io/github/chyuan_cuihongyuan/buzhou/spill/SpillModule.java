package io.github.chyuan_cuihongyuan.buzhou.spill;

import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SkillResourceResolver;

import java.nio.file.Path;
import java.util.List;

public final class SpillModule {

    private final DiskSpillStore store;
    private final SpillService service;
    private SkillResourceResolver skillResourceResolver;

    public SpillModule(Path rootDir, int previewChars, int listPreviewItems) {
        this.store = new DiskSpillStore(rootDir);
        this.service = new SpillService(store, previewChars, listPreviewItems);
    }

    public static SpillModule withDefaults(Path rootDir) {
        return new SpillModule(rootDir, 2048, 20);
    }

    /**
     * 注入 Skill 资源解析器（spec 04：read_range 接管 {@code skill://} 路径）。
     *
     * <p>由 buzhou-skills 的 {@code SkillModule.skillResourceResolver()} 提供，装配期接线，
     * 无 feature→feature Maven 依赖。不注入时 {@code skill://} 路径返回接线提示文本。
     */
    public SpillModule skillResourceResolver(SkillResourceResolver resolver) {
        this.skillResourceResolver = resolver;
        return this;
    }

    /**
     * impl-35 / spec 13 §stores-6：spill 文件按会话清理的 SessionCleaner 贡献者——
     * 会话经资源注册表 close 即删（既有路径）；本贡献者供独立 SessionCleaner /
     * {@code RuntimeConfig.cleanupContributors(...)} 离线级联（会话已 close 后补删）用。
     */
    public io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleanupContributor cleanupContributor(
            String agentName) {
        return io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleanupContributor.of(
                "spill-files",
                sessionId -> store.deleteBySession(sanitize(agentName), sanitize(sessionId)));
    }

    public SpillService service() {
        return service;
    }

    public DiskSpillStore store() {
        return store;
    }

    public RuntimeConfig configure() {
        return new RuntimeConfig(List.of(), java.util.Set.of(), java.util.Set.of(), null,
                List.of(new ReadRangeTool(service, skillResourceResolver)), java.util.Map.of(),
                List.of((registry, appId, agentName, sessionId) ->
                        registry.register("spill-cleanup",
                                () -> store.deleteBySession(
                                        sanitize(agentName), sanitize(sessionId)))));
    }

    private String sanitize(String component) {
        return component.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
