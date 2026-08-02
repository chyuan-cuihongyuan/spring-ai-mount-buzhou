package io.github.chyuan_cuihongyuan.buzhou.spill;

import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;

import java.nio.file.Path;
import java.util.List;

public final class SpillModule {

    private final DiskSpillStore store;
    private final SpillService service;

    public SpillModule(Path rootDir, int previewChars, int listPreviewItems) {
        this.store = new DiskSpillStore(rootDir);
        this.service = new SpillService(store, previewChars, listPreviewItems);
    }

    public static SpillModule withDefaults(Path rootDir) {
        return new SpillModule(rootDir, 2048, 20);
    }

    public SpillService service() {
        return service;
    }

    public DiskSpillStore store() {
        return store;
    }

    public RuntimeConfig configure() {
        return new RuntimeConfig(List.of(), java.util.Set.of(), java.util.Set.of(), null,
                List.of(new ReadRangeTool(service)), java.util.Map.of(),
                List.of((registry, appId, agentName, sessionId) ->
                        registry.register("spill-cleanup",
                                () -> store.deleteBySession(
                                        sanitize(agentName), sanitize(sessionId)))));
    }

    private String sanitize(String component) {
        return component.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
