package io.github.chyuan_cuihongyuan.buzhou.core.retention;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * 文件咨询锁（spec 182 §A / T539，ShedLock 借鉴）：多实例部署里「定时任务
 * 单实例执行」的防线——锁文件以 {@code File.createNewFile()} 的原子性抢锁
 * （同刻只有一个成功），内容 = 持有者 id + 获取时刻。spec 127 fog「多实例
 * 节流」的通用底座（ArchivePurgeJob/巡检犬等多实例各跑一份的问题由此收口）。
 *
 * <p><b>口径（诚实声明）</b>：咨询锁不是互斥锁——防「两个实例同刻开始同一
 * 定时任务」，不防持锁进程僵死（崩溃残留靠 {@code stale} 判定 + 手动/定时
 * 回收，持有者存活不校验——进程心跳是另一族能力）；网络文件系统的
 * createNewFile 原子性以平台语义为准（本地盘/主流分布式 FS 成立）。
 */
public final class AdvisoryFileLock {

    private final Path lockFile;

    public AdvisoryFileLock(Path lockFile) {
        if (lockFile == null) {
            throw new IllegalArgumentException("lockFile must not be null");
        }
        this.lockFile = lockFile;
    }

    /**
     * 抢锁（原子）：成功写入「owner + 时刻」并 true；已被持有 false。
     *
     * @param ownerId 持有者标识（实例 id——非空白）
     */
    public boolean tryAcquire(String ownerId, Instant now) throws IOException {
        requireOwner(ownerId);
        requireNow(now);
        if (!lockFile.toFile().createNewFile()) {
            return false;
        }
        Files.writeString(lockFile, ownerId + "\n" + now,
                StandardCharsets.UTF_8);
        return true;
    }

    /**
     * 释放（仅持有者）：锁文件内容 owner 不符 = 不是你的锁——拒绝释放
     * （防误删他实例的锁）。返回是否实际释放。
     */
    public boolean release(String ownerId) throws IOException {
        requireOwner(ownerId);
        if (!Files.exists(lockFile)) {
            return false;
        }
        String owner = readOwner();
        if (!ownerId.equals(owner)) {
            return false;
        }
        Files.deleteIfExists(lockFile);
        return true;
    }

    /** 当前持有者（无锁 empty——诚实空值）。 */
    public Optional<String> owner() throws IOException {
        if (!Files.exists(lockFile)) {
            return Optional.empty();
        }
        return Optional.ofNullable(readOwner());
    }

    /**
     * 陈旧判定：锁存在且持有超过 ttl（持有者僵死/崩溃残留的回收面）。
     * 返回陈旧锁的持有者 id（不陈旧/无锁 empty）。
     */
    public Optional<String> stale(Instant now, Duration ttl) throws IOException {
        requireNow(now);
        if (ttl == null || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be non-negative");
        }
        if (!Files.exists(lockFile)) {
            return Optional.empty();
        }
        String content = Files.readString(lockFile, StandardCharsets.UTF_8);
        int newline = content.indexOf('\n');
        if (newline < 0) {
            return Optional.of(content); // 损坏锁：视为陈旧可回收（持有者未知给原文）
        }
        String ownerId = content.substring(0, newline);
        Instant acquiredAt = Instant.parse(content.substring(newline + 1));
        return Duration.between(acquiredAt, now).compareTo(ttl) > 0
                ? Optional.of(ownerId) : Optional.empty();
    }

    /** 强制回收（陈旧锁的处置动作；返回是否实际删除）。 */
    public boolean forceRelease() throws IOException {
        return Files.deleteIfExists(lockFile);
    }

    private String readOwner() throws IOException {
        String content = Files.readString(lockFile, StandardCharsets.UTF_8);
        int newline = content.indexOf('\n');
        return newline < 0 ? content : content.substring(0, newline);
    }

    private static void requireOwner(String ownerId) {
        if (ownerId == null || ownerId.isBlank() || ownerId.contains("\n")) {
            throw new IllegalArgumentException("ownerId must be non-blank single-line");
        }
    }

    private static void requireNow(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
    }
}
