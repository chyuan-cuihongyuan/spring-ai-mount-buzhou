package io.github.chyuan_cuihongyuan.buzhou.core.fs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 追加式 JSONL 大小轮转 writer（spec 642 / T934–T935，Logback
 * {@code RollingFileAppender} / logrotate 大小轮转思想）：当前文件 + 待写行
 * 超过 {@code maxBytes} 即轮转——代际 shift（{@code file → file.1}，
 * {@code file.1 → file.2} …，超 {@code maxHistory} 删最老）后重开追加。
 *
 * <p><b>默认开</b>（{@link #DEFAULT_MAX_BYTES} × {@link #DEFAULT_MAX_HISTORY}
 * ≈ 256MB/文件组封顶）——资源保护是缺陷补全非行为变化；{@code maxBytes} /
 * {@code maxHistory} ≤ 0 = 显式关（旧无界追加语义 escape hatch）。
 *
 * <p><b>降级</b>：轮转 IO 失败 best-effort——重开原文件继续写（旁路观测不
 * 放大主链故障），{@link #rotations()} / {@link #rotationFailures()} 计数可观测。
 * 大小内存记账（打开时以现存文件大小初始化），append 无额外系统调用。
 *
 * <p>线程安全：{@link #appendLine} 同步（行完整性优先——观测明细追加频率
 * 事件级，锁竞争非热点）。
 */
public final class RollingJsonlWriter implements AutoCloseable {

    /** 默认大小上限（64MB——观测明细行 ~200B 量级下 ≈ 30 万行/代）。 */
    public static final long DEFAULT_MAX_BYTES = 64L * 1024 * 1024;

    /** 默认代际数（当前 + 3 代历史 ≈ 256MB/文件组封顶）。 */
    public static final int DEFAULT_MAX_HISTORY = 3;

    private final Path path;
    private final long maxBytes;
    private final int maxHistory;
    private BufferedWriter writer;
    private long bytesWritten;
    private final AtomicLong rotations = new AtomicLong();
    private final AtomicLong rotationFailures = new AtomicLong();

    /**
     * @param path       目标 JSONL 文件（父目录自动创建；打开失败上抛——启动期 fail-fast）
     * @param maxBytes   单代大小上限（≤ 0 = 关轮转：无界追加）
     * @param maxHistory 轮转保留代数（≤ 0 = 关轮转）
     */
    public RollingJsonlWriter(Path path, long maxBytes, int maxHistory) throws IOException {
        this.path = path.toAbsolutePath();
        this.maxBytes = maxBytes;
        this.maxHistory = maxHistory;
        Path parent = this.path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        this.writer = Files.newBufferedWriter(this.path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        this.bytesWritten = Files.exists(this.path) ? Files.size(this.path) : 0L;
    }

    /** 追加一行（调用方保证行内换行已转义——JSONL 语义）；每行 flush（tail -f 可观察）。 */
    public synchronized void appendLine(String line) throws IOException {
        if (rollingEnabled() && bytesWritten + lineBytes(line) > maxBytes) {
            rotate();
        }
        writer.write(line);
        writer.newLine();
        writer.flush();
        bytesWritten += lineBytes(line) + 1L; // +1 = newLine（\n）
    }

    /** 轮转是否启用（maxBytes 与 maxHistory 均为正）。 */
    public boolean rollingEnabled() {
        return maxBytes > 0 && maxHistory > 0;
    }

    /** 已发生的轮转次数。 */
    public long rotations() {
        return rotations.get();
    }

    /** 轮转失败次数（best-effort 降级继续写原文件——非零持续增长 = 轮转路径有病灶）。 */
    public long rotationFailures() {
        return rotationFailures.get();
    }

    public Path path() {
        return path;
    }

    /** 当前代已写字节（轮转判定同源记账——观测/测试用）。 */
    public synchronized long bytesWritten() {
        return bytesWritten;
    }

    @Override
    public synchronized void close() throws IOException {
        writer.close();
    }

    /**
     * 静态轮转检查（逐次开写的调用方——PromptUsageJsonl 同族）：现存文件 +
     * 即将写入的估算字节数超限即执行同款代际 shift（与长驻路径同一代语义）。
     * 文件不存在或未超限 = no-op。
     */
    public static int rotateIfNeeded(Path path, long incomingBytes,
            long maxBytes, int maxHistory) throws IOException {
        if (maxBytes <= 0 || maxHistory <= 0 || !Files.exists(path)) {
            return 0;
        }
        if (Files.size(path) + incomingBytes <= maxBytes) {
            return 0;
        }
        shiftGenerations(path, maxHistory);
        // spec 648：静态轮转路径同发指标（与长驻 rotate() 同口径）
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                .counter(METRIC_ROTATED, "file", path.getFileName().toString());
        return 1;
    }

    /** 轮转成功指标名（spec 648——BuzhouMetricsHolder 全局面；tag file=目标文件名）。 */
    public static final String METRIC_ROTATED = "buzhou.jsonl.rotated";
    /** 轮转失败指标名（spec 648——best-effort 降级的运行病灶信号）。 */
    public static final String METRIC_ROTATE_FAILED = "buzhou.jsonl.rotate-failed";

    private void rotate() throws IOException {
        try {
            writer.close();
            shiftGenerations(path, maxHistory);
            writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            bytesWritten = 0L;
            rotations.incrementAndGet();
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                    .counter(METRIC_ROTATED, "file", path.getFileName().toString());
        } catch (IOException rotateFailed) {
            rotationFailures.incrementAndGet();
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                    .counter(METRIC_ROTATE_FAILED, "file", path.getFileName().toString());
            // best-effort：重开原文件继续写（轮转失败不放大——旁路语义）
            writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    /** 代际 shift：file.(h-1)→file.h 自高向低（最老删除），最后 file→file.1。 */
    private static void shiftGenerations(Path path, int maxHistory) throws IOException {
        for (int gen = maxHistory; gen >= 2; gen--) {
            Path from = generationPath(path, gen - 1);
            Path to = generationPath(path, gen);
            if (gen == maxHistory) {
                Files.deleteIfExists(to); // 超龄最老代先清（腾位）
            }
            if (Files.exists(from)) {
                Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        Files.move(path, generationPath(path, 1), StandardCopyOption.REPLACE_EXISTING);
    }

    private static Path generationPath(Path path, int gen) {
        return path.resolveSibling(path.getFileName() + "." + gen);
    }

    private static long lineBytes(String line) {
        return line.getBytes(StandardCharsets.UTF_8).length;
    }
}
