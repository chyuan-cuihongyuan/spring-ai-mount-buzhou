package io.github.chyuan_cuihongyuan.buzhou.core.export;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 导出族合流打包（spec 317 / T625，tarball/OCI artifact 借鉴——多载荷一清单）：
 * 把多个命名 JSONL 导出源打进一个 ZIP（条目级 DEFLATE）；首条目
 * {@code manifest.json} = [{name, lines, sha256}]——离线对账免解压全量（193
 * 防篡改清单同源思想）。单源异常逐源隔离（error 条目留痕，其余继续——换班
 * 不因单源故障全废）；空源零条目诚实。
 */
public final class ExportBundle {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    /** manifest 条目（首条目 manifest.json 的行——对账清单）。 */
    public record ManifestEntry(String name, long lines, String sha256, String error) {
    }

    /** 导出源（与 PiiHitStatsJsonl/ModelCostLedgerJsonl 等静态导出同构）。 */
    @FunctionalInterface
    public interface ExportSource {
        long write(Writer out) throws IOException;
    }

    private ExportBundle() {
    }

    /**
     * 打包：逐命名源写入 ZIP 条目 + manifest 首条目（含每源行数与 sha256；
     * 故障源记 error 不中断）。
     *
     * @param zip    目标文件（父目录自动创建）
     * @param sources 有序命名源（LinkedHashMap 保序）
     * @return manifest 条目（与文件内容一致——对账面）
     */
    public static java.util.List<ManifestEntry> bundle(Path zip,
            LinkedHashMap<String, ExportSource> sources) throws IOException {
        return bundle(zip, sources, Durability.NONE);
    }

    /**
     * spec 621 / T892：带落盘持久档的打包（sqlite WAL 同步档位语义——NONE=close 即返
     * （OS 页缓存，默认零变化）；FILE=zip 数据+元数据 force 到设备；FILE_AND_DIR=另
     * force 父目录 fsync——崩溃后目录项可见，真正的 FULL 档（审计/合规归档用）。
     */
    public static java.util.List<ManifestEntry> bundle(Path zip,
            LinkedHashMap<String, ExportSource> sources, Durability durability) throws IOException {
        Path parent = zip.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        java.util.List<ManifestEntry> manifest = new java.util.ArrayList<>();
        Map<String, byte[]> entries = new LinkedHashMap<>();
        for (Map.Entry<String, ExportSource> source : sources.entrySet()) {
            String name = source.getKey();
            try {
                StringWriter buffer = new StringWriter();
                long lines = source.getValue().write(buffer);
                byte[] content = buffer.toString().getBytes(StandardCharsets.UTF_8);
                entries.put(name, content);
                manifest.add(new ManifestEntry(name, lines, sha256(content), null));
            } catch (RuntimeException | IOException e) {
                entries.put(name + ".error", (e.getClass().getSimpleName() + ": " + e.getMessage())
                        .getBytes(StandardCharsets.UTF_8));
                manifest.add(new ManifestEntry(name, 0L, null,
                        e.getClass().getSimpleName() + ": " + e.getMessage()));
            }
        }
        try (ZipOutputStream out = new ZipOutputStream(
                Files.newOutputStream(zip), StandardCharsets.UTF_8)) {
            out.putNextEntry(new ZipEntry("manifest.json"));
            out.write(manifestJson(manifest).getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                out.putNextEntry(new ZipEntry(entry.getKey()));
                out.write(entry.getValue());
                out.closeEntry();
            }
        }
        if (durability == null) {
            durability = Durability.NONE;
        }
        if (durability != Durability.NONE) {
            try (java.nio.channels.FileChannel ch = java.nio.channels.FileChannel.open(zip,
                    java.nio.file.StandardOpenOption.READ, java.nio.file.StandardOpenOption.WRITE)) {
                ch.force(true);
            }
            if (durability == Durability.FILE_AND_DIR && parent != null) {
                // 目录 fsync 是 POSIX 语义——Windows 无法以 FileChannel 打开目录，
                // best-effort 忽略（E 会话 543 跨平台实证：否则 Windows 全挂）
                try (java.nio.channels.FileChannel dir = java.nio.channels.FileChannel.open(parent,
                        java.nio.file.StandardOpenOption.READ)) {
                    dir.force(true);
                } catch (IOException ignored) {
                    // 平台不支持目录 fsync——跳过（FILE 层 force 已完成）
                }
            }
        }
        return java.util.List.copyOf(manifest);
    }

    /** spec 621：落盘持久档（NONE 默认零变化 / FILE / FILE_AND_DIR）。 */
    public enum Durability {
        NONE, FILE, FILE_AND_DIR
    }

    /** spec 644 / T938：Jackson 序列化（LinkedHashMap 插入序 = 字段序稳定；error 多行消息合法转义——旧土法 replace 丢信息且不处理换行）。 */
    private static String manifestJson(java.util.List<ManifestEntry> manifest) {
        java.util.List<Map<String, Object>> entries = new java.util.ArrayList<>(manifest.size());
        for (ManifestEntry entry : manifest) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", entry.name());
            row.put("lines", entry.lines());
            if (entry.sha256() != null) {
                row.put("sha256", entry.sha256());
            }
            if (entry.error() != null) {
                row.put("error", entry.error());
            }
            entries.add(row);
        }
        try {
            return MAPPER.writeValueAsString(entries);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // manifest 值均为简单类型——序列化失败属不可能分支；兜底上抛不静默吞
            throw new IllegalStateException("manifest 序列化失败", e);
        }
    }

    private static String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16))
                        .append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    /** manifest 行数计算便利（源侧已知行数时免重扫）。 */
    public static long countLines(String content) {
        if (content == null || content.isEmpty()) {
            return 0L;
        }
        return content.chars().filter(c -> c == '\n').count()
                + (content.charAt(content.length() - 1) != '\n' ? 1 : 0);
    }
}
