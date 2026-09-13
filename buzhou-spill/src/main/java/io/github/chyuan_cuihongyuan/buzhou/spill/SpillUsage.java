package io.github.chyuan_cuihongyuan.buzhou.spill;

/**
 * impl-764 / spec 1011：spill 磁盘占用只读快照（Redis INFO memory /
 * PostgreSQL pg_database_size 借鉴——存储占用是一等运维读数）。
 *
 * @param totalBytes 数据文件字节总量（与配额守卫 totalSpillBytes 同口径）
 * @param entryCount spill 条目数（数据文件计数）
 */
public record SpillUsage(long totalBytes, int entryCount) {
}
