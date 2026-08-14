package io.github.chyuan_cuihongyuan.buzhou.spill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

public class DiskSpillStore implements SpillStore {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DATA_SUFFIX = ".spill";
    private static final String META_SUFFIX = ".meta";

    private final Path rootDir;

    /** impl-38 / spec 13 §growth-8：磁盘配额（默认不限）。 */
    private final SpillQuota quota;

    /** impl-41 / spec 13 §T66：spill 句柄泄漏登记（数据文件路径 → handle；删除路径解除）。 */
    private final java.util.concurrent.ConcurrentHashMap<Path,
            io.github.chyuan_cuihongyuan.buzhou.core.leak.ResourceLeakDetector.LeakHandle> leakHandles =
            new java.util.concurrent.ConcurrentHashMap<>();

    public DiskSpillStore(Path rootDir) {
        this(rootDir, SpillQuota.unbounded());
    }

    public DiskSpillStore(Path rootDir, SpillQuota quota) {
        this.rootDir = rootDir;
        this.quota = quota == null ? SpillQuota.unbounded() : quota;
    }

    @Override
    public synchronized SpillHandle store(SpillEntry entry, int previewChars) {
        Path dataPath = dataPath(entry.uri());
        if (Files.exists(dataPath)) {
            throw new IllegalStateException("Spill already exists (one call one spill): " + entry.uri());
        }
        leakHandles.put(dataPath, io.github.chyuan_cuihongyuan.buzhou.core.leak
                .LeakDetectorHolder.detector().track("spill:" + entry.uri()));
        enforceQuota(entry);
        try {
            Files.createDirectories(dataPath.getParent());
            writeAtomically(dataPath, entry.content());
            writeAtomically(metaPath(entry.uri()), metaJson(entry, false));
            // impl-41 / spec 13 §T66：spill 指标（outcome=spilled；degraded/failed 在服务层）
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                    .counter("buzhou.spill.requests", "outcome", "spilled");
            return new SpillHandle(entry.uri(), entry.sizeChars(),
                    RangeReadEngine.previewOf(entry.content(), previewChars, 20));
        } catch (IOException e) {
            throw new UncheckedIOException("Spill store failed: " + entry.uri(), e);
        }
    }

    /**
     * impl-38 / spec 13 §growth-8：配额守卫（拒绝落盘，原文回喂——由
     * {@code SpillService.tryOffload} 的 degraded 路径透传并提示模型走显式分页）。
     * 计量口径：数据文件字符数 × UTF-8 近似 1 字节（配额是护栏不是计费）。
     */
    private void enforceQuota(SpillEntry entry) {
        if (quota.maxFilesPerSession() != null) {
            Path sessionDir = dataPath(entry.uri()).getParent();
            int existing = countSpillFiles(sessionDir);
            if (existing >= quota.maxFilesPerSession()) {
                throw new io.github.chyuan_cuihongyuan.buzhou.core.error.QuotaExceededException(
                        ("spill 单会话文件数已达上限 maxFilesPerSession=%d（sessionId=%s，现有 %d）："
                                + "拒绝落盘，原文回喂——请用 read_range 显式分页读取")
                                .formatted(quota.maxFilesPerSession(), entry.uri().sessionId(), existing));
            }
        }
        if (quota.maxTotalBytes() != null) {
            long current = totalSpillBytes();
            if (current + entry.sizeChars() > quota.maxTotalBytes()) {
                throw new io.github.chyuan_cuihongyuan.buzhou.core.error.QuotaExceededException(
                        ("spill 总量将超上限 maxTotalBytes=%d（当前 %d，拟写入 %d）："
                                + "拒绝落盘，原文回喂——请用 read_range 显式分页读取")
                                .formatted(quota.maxTotalBytes(), current, entry.sizeChars()));
            }
        }
    }

    private int countSpillFiles(Path sessionDir) {
        if (sessionDir == null || !Files.isDirectory(sessionDir)) {
            return 0;
        }
        try (Stream<Path> files = Files.list(sessionDir)) {
            return (int) files.filter(p -> p.toString().endsWith(DATA_SUFFIX)).count();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private long totalSpillBytes() {
        if (!Files.isDirectory(rootDir)) {
            return 0;
        }
        try (Stream<Path> walk = Files.walk(rootDir)) {
            return walk.filter(p -> p.toString().endsWith(DATA_SUFFIX))
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * impl-38 / spec 13 §growth-8：启动孤儿扫描——引用会话不存在的 spill 目录
     * （会话数据已被级联删除/保留策略清理，磁盘文件残留）整目录清理，返回删除的
     * spill 文件数。幂等（重复扫描无孤儿可删即返回 0）。
     *
     * @param liveSessionIds 仍存在的会话集合（目录名匹配）
     */
    public int sweepOrphans(java.util.Set<String> liveSessionIds) {
        if (!Files.isDirectory(rootDir)) {
            return 0;
        }
        java.util.Set<String> live = liveSessionIds == null ? java.util.Set.of() : liveSessionIds;
        int deleted = 0;
        try (Stream<Path> agents = Files.list(rootDir)) {
            for (Path agentDir : agents.filter(Files::isDirectory).toList()) {
                try (Stream<Path> sessions = Files.list(agentDir)) {
                    for (Path sessionDir : sessions.filter(Files::isDirectory).toList()) {
                        if (live.contains(sessionDir.getFileName().toString())) {
                            continue;
                        }
                        try (Stream<Path> files = Files.list(sessionDir)) {
                            for (Path file : files.filter(p -> p.toString().endsWith(DATA_SUFFIX)).toList()) {
                                deleteQuietly(file);
                                deleteQuietly(Path.of(file.toString().replace(DATA_SUFFIX, META_SUFFIX)));
                                deleted++;
                            }
                        }
                        deleteQuietly(sessionDir);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return deleted;
    }

    /**
     * impl-17 / T45：完整性复验——当前数据文件 sha256 与落盘时 meta 记录的摘要比对
     * （git 惯例：读回重算必校验；腐化/TOCTOU 可检测）。meta 缺失或无摘要字段 = 无法复验，
     * 返回 true（向后兼容旧条目）。
     */
    public boolean verifyIntegrity(SpillUri uri) {
        try {
            Path meta = metaPath(uri);
            if (!Files.exists(meta)) {
                return true;
            }
            var node = MAPPER.readTree(Files.readString(meta));
            String recorded = node.path("contentSha256").asText(null);
            if (recorded == null || recorded.isBlank()) {
                return true;
            }
            return ReadIntegrity.sha256(load(uri).orElse("")).equals(recorded);
        } catch (Exception e) {
            return true; // 复验通道自身故障不影响读路径（读侧 lenient）
        }
    }

    @Override
    public Optional<String> load(SpillUri uri) {
        try {
            Path path = dataPath(uri);
            return Files.exists(path) ? Optional.of(Files.readString(path)) : Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public RangeReadResult readRange(SpillUri uri, RangeReadRequest request) {
        // impl-17 / T45：读回复验——不一致时内容前缀完整性告警（读侧 lenient=warning 透传）
        if (!verifyIntegrity(uri)) {
            return load(uri)
                    .map(content -> new RangeReadResult(
                            ReadIntegrity.CORRUPTION_WARNING + "\n"
                                    + RangeReadEngine.read(content, request).content(),
                            content.length(), false, null))
                    .orElse(new RangeReadResult("spill 不存在或已被清理：" + uri, 0, false, null));
        }
        return load(uri)
                .map(content -> RangeReadEngine.read(content, request))
                .orElse(new RangeReadResult("spill 不存在或已被清理：" + uri, 0, false, null));
    }

    @Override
    public void markLinked(SpillUri uri) {
        Path metaPath = metaPath(uri);
        if (!Files.exists(metaPath)) {
            return;
        }
        try {
            ObjectNode meta = (ObjectNode) MAPPER.readTree(Files.readString(metaPath));
            meta.put("linked", true);
            writeAtomically(metaPath, MAPPER.writeValueAsString(meta));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void delete(SpillUri uri) {
        closeLeakHandle(dataPath(uri));
        deleteQuietly(dataPath(uri));
        deleteQuietly(metaPath(uri));
    }

    private void closeLeakHandle(Path dataPath) {
        io.github.chyuan_cuihongyuan.buzhou.core.leak.ResourceLeakDetector.LeakHandle handle =
                leakHandles.remove(dataPath);
        if (handle != null) {
            handle.close();
        }
    }

    @Override
    public int deleteBySession(String agentName, String sessionId) {
        Path sessionDir = rootDir.resolve(agentName).resolve(sessionId);
        if (!Files.isDirectory(sessionDir)) {
            return 0;
        }
        int[] count = {0};
        try (Stream<Path> files = Files.list(sessionDir)) {
            files.filter(p -> p.toString().endsWith(DATA_SUFFIX)).forEach(p -> {
                closeLeakHandle(p);
                deleteQuietly(p);
                deleteQuietly(Path.of(p.toString().replace(DATA_SUFFIX, META_SUFFIX)));
                count[0]++;
            });
            Files.deleteIfExists(sessionDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return count[0];
    }

    @Override
    public int deleteExpired(Instant now, Duration ttl) {
        if (!Files.isDirectory(rootDir)) {
            return 0;
        }
        java.util.List<Path> metas;
        try (Stream<Path> walk = Files.walk(rootDir)) {
            metas = walk.filter(p -> p.toString().endsWith(META_SUFFIX)).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        int count = 0;
        for (Path metaPath : metas) {
            try {
                ObjectNode meta = (ObjectNode) MAPPER.readTree(Files.readString(metaPath));
                boolean linked = meta.path("linked").asBoolean(false);
                Instant createdAt = Instant.parse(meta.path("createdAt").asText());
                if (!linked && createdAt.plus(ttl).isBefore(now)) {
                    closeLeakHandle(Path.of(metaPath.toString().replace(META_SUFFIX, DATA_SUFFIX)));
                    deleteQuietly(metaPath);
                    deleteQuietly(Path.of(metaPath.toString().replace(META_SUFFIX, DATA_SUFFIX)));
                    count++;
                }
            } catch (Exception ignored) {
            }
        }
        return count;
    }

    @Override
    public boolean exists(SpillUri uri) {
        return Files.exists(dataPath(uri));
    }

    public Path dataPathOf(SpillUri uri) {
        return dataPath(uri);
    }

    private Path dataPath(SpillUri uri) {
        return rootDir.resolve(uri.agentName()).resolve(uri.sessionId())
                .resolve(uri.toolCallId() + DATA_SUFFIX);
    }

    private Path metaPath(SpillUri uri) {
        return rootDir.resolve(uri.agentName()).resolve(uri.sessionId())
                .resolve(uri.toolCallId() + META_SUFFIX);
    }

    private void writeAtomically(Path target, String content) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(tmp, content);
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
    }

    private String metaJson(SpillEntry entry, boolean linked) {
        ObjectNode meta = MAPPER.createObjectNode();
        meta.put("sizeChars", entry.sizeChars());
        meta.put("contentType", entry.contentType());
        meta.put("createdAt", entry.createdAt().toString());
        meta.put("linked", linked);
        // impl-17 / T45：落盘即记录 whole-content sha256（读回复验锚点）
        meta.put("contentSha256", ReadIntegrity.sha256(entry.content()));
        return meta.toString();
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
