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
    private static final System.Logger LOGGER = System.getLogger(DiskSpillStore.class.getName());

    private final Path rootDir;

    /** impl-38 / spec 13 §growth-8：磁盘配额（默认不限）。 */
    private final SpillQuota quota;

    /** impl-41 / spec 13 §T66：spill 句柄泄漏登记（数据文件路径 → handle；删除路径解除）。 */
    private final java.util.concurrent.ConcurrentHashMap<Path,
            io.github.chyuan_cuihongyuan.buzhou.core.leak.ResourceLeakDetector.LeakHandle> leakHandles =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** spec 26 / T105 / impl-80：证据引用账本（fork 引用计数共享——最后引用者关闭）。 */
    private final EvidenceRefLedger refLedger;

    /** spec 40 §A / T151 / impl-122：落盘静态加密（null = 直通，零行为变化）。 */
    private final SpillCipher cipher;

    public DiskSpillStore(Path rootDir) {
        this(rootDir, SpillQuota.unbounded());
    }

    public DiskSpillStore(Path rootDir, SpillQuota quota) {
        this(rootDir, quota, null);
    }

    /** spec 40 §A：带静态加密构造——仅 `.spill` 数据文件加密，meta 保持明文（sha256 明文锚点）。 */
    public DiskSpillStore(Path rootDir, SpillQuota quota, SpillCipher cipher) {
        this.rootDir = rootDir;
        this.quota = quota == null ? SpillQuota.unbounded() : quota;
        this.cipher = cipher;
        this.refLedger = new EvidenceRefLedger(rootDir);
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
            writeAtomically(dataPath, cipher == null ? entry.content() : cipher.encrypt(entry.content()));
            writeAtomically(metaPath(entry.uri()), metaJson(entry, false));
            // impl-41 / spec 13 §T66：spill 指标（outcome=spilled；degraded/failed 在服务层）
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                    .counter("buzhou.spill.requests", "outcome", "spilled");
            return new SpillHandle(entry.uri(), entry.sizeChars(),
                    RangeReadEngine.previewOf(entry.content(), previewChars, 20));
        } catch (IOException e) {
            throw new io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException(
                    io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode.SPILL_IO_FAILED,
                    "Spill store failed: " + entry.uri(), e);
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
            throw new io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException(
                    io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode.SPILL_IO_FAILED,
                    "spill 磁盘 IO 失败", e);
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
            throw new io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException(
                    io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode.SPILL_IO_FAILED,
                    "spill 磁盘 IO 失败", e);
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
                        int retained = 0;
                        try (Stream<Path> files = Files.list(sessionDir)) {
                            for (Path file : files.filter(p -> p.toString().endsWith(DATA_SUFFIX)).toList()) {
                                if (!refLedger.referrers(uriOf(file)).isEmpty()) {
                                    retained++; // fork 仍引用（属主会话已亡）：物理保留
                                    continue;
                                }
                                deleteQuietly(file);
                                deleteQuietly(Path.of(file.toString().replace(DATA_SUFFIX, META_SUFFIX)));
                                deleted++;
                            }
                        }
                        if (retained == 0) {
                            deleteQuietly(sessionDir);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException(
                    io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode.SPILL_IO_FAILED,
                    "spill 磁盘 IO 失败", e);
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
            if (!Files.exists(path)) {
                return Optional.empty();
            }
            // spec 40 §A：加密文件解密（魔法探测），旧明文直通——调用方拿到的恒为明文
            String raw = Files.readString(path);
            return Optional.of(cipher == null ? raw : cipher.decryptIfEncrypted(raw));
        } catch (IOException e) {
            throw new io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException(
                    io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode.SPILL_IO_FAILED,
                    "spill 磁盘 IO 失败", e);
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
                    .orElse(new RangeReadResult(
                        "EVIDENCE_GONE：spill 证据已被清理（" + uri + "）——该证据可能属已删除会话"
                                + "且无引用保留。请基于对话摘要重建所需信息，或重新执行生成该数据的工具。",
                        0, false, null));
        }
        return load(uri)
                .map(content -> RangeReadEngine.read(content, request))
                .orElse(new RangeReadResult(
                        "EVIDENCE_GONE：spill 证据已被清理（" + uri + "）——该证据可能属已删除会话"
                                + "且无引用保留。请基于对话摘要重建所需信息，或重新执行生成该数据的工具。",
                        0, false, null));
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
            throw new io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException(
                    io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode.SPILL_IO_FAILED,
                    "spill 磁盘 IO 失败", e);
        }
    }

    @Override
    public void delete(SpillUri uri) {
        refLedger.remove(uri.toString());
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

    /**
     * 会话级联删除（spec 26 / T105 引用感知）：①该会话持有的全部引用摘除——引用集清空
     * 的证据（属主早已删除、被本会话引用保留至今）此刻物理删；②属主目录内文件——仍有
     * 其他会话引用（fork 存活）则保留 + WARN，否则物理删；目录空了才移除。
     */
    @Override
    public int deleteBySession(String agentName, String sessionId) {
        int count = 0;
        // ① 延迟物理删：本会话是最后引用者的证据
        for (String uri : refLedger.releaseAllFor(sessionId)) {
            try {
                delete(SpillUri.parse(uri));
                count++;
            } catch (IllegalArgumentException ignored) {
                // 账本 uri 与磁盘形态不一致（历史数据）：跳过
            }
        }
        // ② 属主目录清理（引用门控）
        Path sessionDir = rootDir.resolve(agentName).resolve(sessionId);
        if (!Files.isDirectory(sessionDir)) {
            return count;
        }
        try (Stream<Path> files = Files.list(sessionDir)) {
            int retained = 0;
            for (Path p : files.filter(f -> f.toString().endsWith(DATA_SUFFIX)).toList()) {
                String uri = uriOf(p);
                if (!refLedger.referrers(uri).isEmpty()) {
                    retained++;
                    continue; // fork 仍引用：物理保留（最后引用者关闭时删）
                }
                closeLeakHandle(p);
                deleteQuietly(p);
                deleteQuietly(Path.of(p.toString().replace(DATA_SUFFIX, META_SUFFIX)));
                refLedger.remove(uri);
                count++;
            }
            if (retained == 0) {
                Files.deleteIfExists(sessionDir);
            } else {
                LOGGER.log(System.Logger.Level.WARNING,
                        "spill 会话删除保留 " + retained + " 个仍被引用的证据文件（fork 存活）：" + sessionDir);
            }
        } catch (IOException e) {
            throw new io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException(
                    io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode.SPILL_IO_FAILED,
                    "spill 磁盘 IO 失败", e);
        }
        return count;
    }

    /** 从数据文件路径反推 uri（目录名即 sanitize 后的 agent/session，文件名即 toolCallId）。 */
    private String uriOf(Path dataFile) {
        return "spill://" + dataFile.getParent().getParent().getFileName() + "/"
                + dataFile.getParent().getFileName() + "/"
                + dataFile.getFileName().toString().replace(DATA_SUFFIX, "");
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
            throw new io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException(
                    io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode.SPILL_IO_FAILED,
                    "spill 磁盘 IO 失败", e);
        }
        int count = 0;
        for (Path metaPath : metas) {
            try {
                ObjectNode meta = (ObjectNode) MAPPER.readTree(Files.readString(metaPath));
                boolean linked = meta.path("linked").asBoolean(false);
                Instant createdAt = Instant.parse(meta.path("createdAt").asText());
                String uri = uriOf(Path.of(metaPath.toString().replace(META_SUFFIX, DATA_SUFFIX)));
                boolean referenced = !refLedger.referrers(uri).isEmpty();
                if (!linked && !referenced && createdAt.plus(ttl).isBefore(now)) {
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

    /**
     * spec 26 / T105 / impl-80：fork 引用登记——sessionId 的全部证据（任意 agent 目录下）
     * 为 referrer 增加引用。按 sessionId 全根扫描（会话目录名全局唯一约定），免 agentName 传递。
     */
    public int acquireSessionReferences(String sessionId, String referrerSessionId) {
        String dirName = SpillModule.sanitizeComponent(sessionId);
        int acquired = 0;
        if (!Files.isDirectory(rootDir)) {
            return 0;
        }
        try (Stream<Path> agents = Files.list(rootDir)) {
            for (Path agentDir : agents.filter(Files::isDirectory).toList()) {
                Path sessionDir = agentDir.resolve(dirName);
                if (!Files.isDirectory(sessionDir)) {
                    continue;
                }
                try (Stream<Path> files = Files.list(sessionDir)) {
                    for (Path file : files.filter(p -> p.toString().endsWith(DATA_SUFFIX)).toList()) {
                        refLedger.acquire(uriOf(file), SpillModule.sanitizeComponent(referrerSessionId));
                        acquired++;
                    }
                }
            }
        } catch (IOException e) {
            throw new io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException(
                    io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode.SPILL_IO_FAILED,
                    "spill 磁盘 IO 失败", e);
        }
        return acquired;
    }

    /** 观测/测试：证据当前引用会话集合（空集 = 无引用登记）。 */
    public java.util.Set<String> evidenceReferrers(SpillUri uri) {
        return refLedger.referrers(uri.toString());
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
