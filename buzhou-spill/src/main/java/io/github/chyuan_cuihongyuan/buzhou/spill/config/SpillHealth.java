package io.github.chyuan_cuihongyuan.buzhou.spill.config;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * spill 机制健康（impl-41 / spec 13 §T66）：核心职能 = 长内容可落盘可回读。
 * 探针 = rootDir 一次临时文件写删往返；<b>DOWN 仅当 rootDir 不可写</b>（磁盘满/权限/
 * 路径失效 = 核心职能不可用）；机制未启用报 UNKNOWN。
 */
public final class SpillHealth implements BuzhouHealth {

    private final boolean enabled;
    private final Path rootDir;

    public SpillHealth(boolean enabled, Path rootDir) {
        this.enabled = enabled;
        this.rootDir = rootDir;
    }

    @Override
    public String mechanism() {
        return "spill";
    }

    @Override
    public Status status() {
        if (!enabled) {
            return Status.UNKNOWN;
        }
        try {
            // 首探时建目录（与 DiskSpillStore.store 的 createDirectories 行为一致——
            // 目录不存在 ≠ 不可写，不误报 DOWN）
            Files.createDirectories(rootDir);
            Path probe = Files.createTempFile(rootDir, "buzhou-health-", ".probe");
            Files.deleteIfExists(probe);
            return Status.UP;
        } catch (IOException | RuntimeException e) {
            return Status.DOWN;
        }
    }

    @Override
    public Map<String, Object> details() {
        return Map.of("enabled", enabled, "rootDir", String.valueOf(rootDir),
                "probe", "temp-write-delete");
    }
}
