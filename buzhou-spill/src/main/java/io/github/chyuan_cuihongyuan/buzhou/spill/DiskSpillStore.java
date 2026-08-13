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

    public DiskSpillStore(Path rootDir) {
        this.rootDir = rootDir;
    }

    @Override
    public synchronized SpillHandle store(SpillEntry entry, int previewChars) {
        Path dataPath = dataPath(entry.uri());
        if (Files.exists(dataPath)) {
            throw new IllegalStateException("Spill already exists (one call one spill): " + entry.uri());
        }
        try {
            Files.createDirectories(dataPath.getParent());
            writeAtomically(dataPath, entry.content());
            writeAtomically(metaPath(entry.uri()), metaJson(entry, false));
            return new SpillHandle(entry.uri(), entry.sizeChars(),
                    RangeReadEngine.previewOf(entry.content(), previewChars, 20));
        } catch (IOException e) {
            throw new UncheckedIOException("Spill store failed: " + entry.uri(), e);
        }
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
        deleteQuietly(dataPath(uri));
        deleteQuietly(metaPath(uri));
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
