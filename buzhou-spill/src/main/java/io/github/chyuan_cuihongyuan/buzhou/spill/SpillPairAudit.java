package io.github.chyuan_cuihongyuan.buzhou.spill;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * spill 双文件配对完整性巡检（spec 707 / T1014，Git fsck 悬空对象思想）：
 * store() 两次 writeAtomically 之间的崩溃窗口留下 data-without-meta /
 * meta-without-data——属主会话存活时 sweepOrphans（会话层）永远不扫，
 * 配额被静默吞噬。本面把结构完整性变成可查询证据（只读不删——清理归
 * housekeeper 接线轮）。
 *
 * <p>三层完整性矩阵：sweepOrphans（会话层）→ 本面（文件对层）→
 * ReadIntegrity（内容层，539 读时 sha256）。
 */
public final class SpillPairAudit {

    /** data 文件后缀（与 DiskSpillStore 同约定——独立常量避免跨类耦合）。 */
    private static final String DATA_SUFFIX = ".spill";
    private static final String META_SUFFIX = ".meta";

    /** 单条发现（kind ∈ DATA_WITHOUT_META / META_WITHOUT_DATA；uri 相对根正斜杠）。 */
    public record Finding(String kind, String uri, long bytes) {
    }

    /** 不可变报告（findings 按 uri 字典序；dataBytes=在册 data 文件总字节）。 */
    public record Report(List<Finding> findings, int dataFiles, int metaFiles, long dataBytes) {
    }

    private SpillPairAudit() {
    }

    /** 只读巡检（null fail-fast；root 不存在 = 空 Report——未启用 spill 诚实零）。 */
    public static Report audit(Path rootDir) {
        Objects.requireNonNull(rootDir, "rootDir");
        if (!Files.isDirectory(rootDir)) {
            return new Report(List.of(), 0, 0, 0);
        }
        List<Path> dataFiles = new ArrayList<>();
        List<Path> metaFiles = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(rootDir)) {
            walk.filter(Files::isRegularFile).forEach(file -> {
                String name = file.getFileName().toString();
                if (name.endsWith(DATA_SUFFIX)) {
                    dataFiles.add(file);
                } else if (name.endsWith(META_SUFFIX)) {
                    metaFiles.add(file);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("spill 配对巡检 IO 失败", e);
        }
        List<Finding> findings = new ArrayList<>();
        long dataBytes = 0;
        for (Path data : dataFiles) {
            dataBytes += sizeOf(data);
            Path meta = Path.of(data.toString().replace(DATA_SUFFIX, META_SUFFIX));
            if (!Files.exists(meta)) {
                findings.add(new Finding("DATA_WITHOUT_META", relativize(rootDir, data), sizeOf(data)));
            }
        }
        for (Path meta : metaFiles) {
            Path data = Path.of(meta.toString().replace(META_SUFFIX, DATA_SUFFIX));
            if (!Files.exists(data)) {
                findings.add(new Finding("META_WITHOUT_DATA", relativize(rootDir, meta), sizeOf(meta)));
            }
        }
        findings.sort(java.util.Comparator.comparing(Finding::uri));
        return new Report(List.copyOf(findings), dataFiles.size(), metaFiles.size(), dataBytes);
    }

    private static long sizeOf(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0; // 读不到尺寸（竞态删除）——不计字节但保留发现
        }
    }

    /** 相对根路径正斜杠归一（跨平台报告一致）。 */
    private static String relativize(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }
}
