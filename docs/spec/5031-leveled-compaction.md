# Spec 5031 — Leveled Compaction 分层压实挑选（effort #5031，S32）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6163–T6164，impl 2182）。
> 借鉴：LevelDB/RocksDB leveled compaction 策略（层容量阶梯思想）。

## Problem Statement

压实调度的病：无容量阶梯时每层无差别扫（写放大失控）或
全量线性扫挑选（决策放大）——**层容量阶梯 + 区间重叠
挑选面**缺失。

## Solution

`LeveledCompaction`（core/recovery）：

- 容量阶梯：L0 固定触发线（4 表）、L_n = 基准(2) ×
  比率(10)^(n-1)——写入放大的空间局部性由阶梯保证；
- `plan()`：全超容层中评分最高者（表数/容量）先挑、并列
  浅层优先；源表取该层最旧（FIFO 注册序），目标表按键
  区间重叠收集（firstKey 序，并列 id 定序）；末层同层压实；
- `complete(plan)`：源/目标出账，合并产物由调用方 register
  回账（职责分离——本件只管挑选语义面）；
- 读数：scoreOf/capacityOf/tableCountOf；
- fail-fast：maxLevel 越界、null/空表 id、键区间倒置、
  重复表 id、层级越界、幽灵源表。

## User Stories

1. 作为存储作者，每层容量有上界——写放大有界可算。
2. 作为压实调度者，最旧源表 + 最小重叠集——决策确定性可回放。

## Testing Decisions

- 容量阶梯圣像（4/2/20/200/2000）；无超容返回 null（评分
  0.25 读数）；满层触发：最旧源 + firstKey 序重叠目标；
  最高分赢（1.5 > 1.25）；并列浅层优先（1.25=1.25 选 L0）；
  末层同层压实；complete 出账 + 产物回账；畸形 fail-fast
  （含字符串键字典序语义：k10 < k5 钉住）。

## Out of Scope

- 不做真实表数据合并（本件是挑选/记账语义面）；不做
  size-tiered（R14 已覆盖同族异面）；不做磁盘 IO。

## Further Notes

- 与 KeyCompaction（同键留新日志压缩）同族不同面：键值保留
  语义 vs 层级容量挑选；与 MemTable（spec 5024）衔接：滚动
  产物可作本件 L0 表。Wave 6 第二件。
- 里程碑：S32/50（64%）。
