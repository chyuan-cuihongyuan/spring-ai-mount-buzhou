package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

/**
 * Leveled Compaction 分层压实挑选（spec 5031 / T6163 / impl 2182）——
 * LevelDB/RocksDB leveled 压实策略思想：层级容量阶梯
 * （L0 固定触发线，L_n = 基准 × 比率^(n-1)）驱动挑选——超容层中
 * 最旧表（FIFO 注册序）作源，按键区间重叠收集下一层（末层为
 * 同层）目标表——写入放大的空间局部性由容量阶梯保证（每层
 * 单键至多参与一次层间压实）；压实评分 = 表数/容量（最高分
 * 层先挑，并列浅层优先）。确定性无时间依赖。
 *
 * <p>与 KeyCompaction（同键留新日志压缩）同族不同面：键值保留
 * 语义 vs 层级容量挑选；与 MemTable（spec 5024 满表滚动交接）
 * 衔接：滚动产物可作本件 L0 表。
 */
public final class LeveledCompaction {

    /** 一张不可变段表的键区间指纹（firstKey/lastKey 闭区间）。 */
    public record LevelTable(String tableId, String firstKey, String lastKey) {
        public LevelTable {
            if (tableId == null || tableId.isEmpty()) {
                throw new IllegalArgumentException("tableId 非空");
            }
            if (firstKey == null || lastKey == null || firstKey.compareTo(lastKey) > 0) {
                throw new IllegalArgumentException("键区间非法：" + firstKey + ".." + lastKey);
            }
        }

        boolean overlaps(LevelTable other) {
            return firstKey.compareTo(other.lastKey) <= 0
                    && other.firstKey.compareTo(lastKey) <= 0;
        }
    }

    /** 一次压实挑选：源表 + 键区间重叠的目标表（按 firstKey 序）。 */
    public record CompactionPlan(int sourceLevel, int targetLevel,
                                 String sourceTableId, List<String> targetTableIds) {
    }

    /** L0 压实触发线（LevelDB level0_file_num_compaction_trigger 口径）。 */
    private static final int L0_TRIGGER = 4;

    /** 层容量基准（L1 表数）。 */
    private static final int LEVEL_BASE_TABLES = 2;

    /** 层容量放大比率（相邻层容量倍数）。 */
    private static final int LEVEL_RATIO = 10;

    /** 层深上限（容量阶梯 int 不溢出的最大层号）。 */
    private static final int MAX_LEVEL_BOUND = 9;

    private final int maxLevel;
    private final List<Deque<LevelTable>> levels;

    /** 定构（maxLevel≥1；容量阶梯常量定构——L0=4、L1=2、比率 10）。 */
    public LeveledCompaction(int maxLevel) {
        if (maxLevel < 1 || maxLevel > MAX_LEVEL_BOUND) {
            throw new IllegalArgumentException("maxLevel 1.." + MAX_LEVEL_BOUND + "：" + maxLevel);
        }
        this.maxLevel = maxLevel;
        this.levels = new ArrayList<>();
        for (int i = 0; i <= maxLevel; i++) {
            levels.add(new ArrayDeque<>());
        }
    }

    /** 注册一张段表（重复 id fail-fast；同 id 段表唯一——压实记账前提）。 */
    public void register(LevelTable table, int level) {
        requireLevel(level);
        for (Deque<LevelTable> levelTables : levels) {
            for (LevelTable existing : levelTables) {
                if (existing.tableId().equals(table.tableId())) {
                    throw new IllegalArgumentException("表 id 已存在：" + table.tableId());
                }
            }
        }
        levels.get(level).addLast(table);
    }

    /**
     * 压实挑选：全超容层中评分最高者（并列浅层优先）的最旧表
     * 作源，收集目标层重叠表（firstKey 序）；无层超容返回 null。
     */
    public CompactionPlan plan() {
        int bestLevel = -1;
        double bestScore = 0.0;
        for (int level = 0; level <= maxLevel; level++) {
            double score = scoreOf(level);
            if (score >= 1.0 && (bestLevel < 0 || score > bestScore)) {
                bestScore = score;
                bestLevel = level;
            }
        }
        if (bestLevel < 0) {
            return null;
        }
        LevelTable source = levels.get(bestLevel).peekFirst();
        int targetLevel = Math.min(bestLevel + 1, maxLevel);
        List<LevelTable> overlapped = new ArrayList<>();
        for (LevelTable candidate : levels.get(targetLevel)) {
            if (!candidate.tableId().equals(source.tableId()) && candidate.overlaps(source)) {
                overlapped.add(candidate);
            }
        }
        overlapped.sort(Comparator.comparing(LevelTable::firstKey)
                .thenComparing(LevelTable::tableId));
        List<String> targets = overlapped.stream().map(LevelTable::tableId).toList();
        return new CompactionPlan(bestLevel, targetLevel, source.tableId(), targets);
    }

    /** 完成一次压实：源表与目标表出账（合并产物由调用方 register 回账）。 */
    public void complete(CompactionPlan plan) {
        requireLevel(plan.sourceLevel());
        requireLevel(plan.targetLevel());
        if (!levels.get(plan.sourceLevel()).removeIf(
                t -> t.tableId().equals(plan.sourceTableId()))) {
            throw new IllegalArgumentException("源表不在源层：" + plan.sourceTableId());
        }
        levels.get(plan.targetLevel()).removeIf(
                t -> plan.targetTableIds().contains(t.tableId()));
    }

    /** 压实评分读数（表数/容量；>1 即超容待压实）。 */
    public double scoreOf(int level) {
        requireLevel(level);
        return (double) levels.get(level).size() / capacityOf(level);
    }

    /** 层容量读数（容量阶梯：L0 触发线、L_n=基准×比率^(n-1)）。 */
    public int capacityOf(int level) {
        requireLevel(level);
        if (level == 0) {
            return L0_TRIGGER;
        }
        long capacity = LEVEL_BASE_TABLES;
        for (int i = 1; i < level; i++) {
            capacity *= LEVEL_RATIO;
        }
        return (int) capacity;
    }

    /** 层表数读数。 */
    public int tableCountOf(int level) {
        requireLevel(level);
        return levels.get(level).size();
    }

    private void requireLevel(int level) {
        if (level < 0 || level > maxLevel) {
            throw new IllegalArgumentException("层级越界 0.." + maxLevel + "：" + level);
        }
    }
}
