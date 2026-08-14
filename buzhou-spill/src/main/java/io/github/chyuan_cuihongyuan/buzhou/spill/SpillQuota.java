package io.github.chyuan_cuihongyuan.buzhou.spill;

/**
 * spill 磁盘配额（impl-38 / spec 13 §growth-8）——超限<b>拒绝落盘</b>（noeviction：
 * 原文照常回喂，提示模型走显式分页），绝不静默逐出已落盘内容。
 *
 * @param maxTotalBytes     全部 spill 数据文件字节上限；null/＜1 = 不限
 * @param maxFilesPerSession 单会话 spill 文件数上限；null/＜1 = 不限
 */
public record SpillQuota(Long maxTotalBytes, Integer maxFilesPerSession) {

    public SpillQuota {
        maxTotalBytes = maxTotalBytes == null || maxTotalBytes < 1 ? null : maxTotalBytes;
        maxFilesPerSession = maxFilesPerSession == null || maxFilesPerSession < 1 ? null : maxFilesPerSession;
    }

    public static SpillQuota unbounded() {
        return new SpillQuota(null, null);
    }
}
