# Spec 4013 — 尺寸分层合并挑选（effort #4013，R14）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6027–T6028，impl 2114）。
> 借鉴：Cassandra SizeTieredCompactionStrategy。

## Problem Statement

LSM 压实「新旧混压」（巨块反复重写）与「碎片堆积」（小块永不合并）
两病——尺寸分层的成组裁决件缺失。

## Solution

`SizeTieredMergePicker`（core/cleanup，纯裁决无 IO）：

- 候选按尺寸升序 → 均值 ±radius（默认 1.5）内归同桶；
- 桶员 ≥ minThreshold（默认 4）才成组；成组取 maxThreshold 个
  最小者（单次压实写放大封顶）；
- 多桶竞选取员最多者（小文件优先清，先遇并列保持确定性）；
- 输出尺寸升序（并列 id 序）；无合格组空表诚实。

## User Stories

1. 作为压实作者，同量级小块合并、巨块不被拖累重写。
2. 作为容量作者，单次合并的输入体积有硬顶（max 截断）。

## Testing Decisions

- 四同量级成组 + 巨块异层排除；桶员 3<min 空表 + 桶员 6>max 取
  最小 4；两桶竞选 5 员胜 4 员；空/单候选空表；畸形七型 fail-fast。

## Out of Scope

- 不做 leveled 压实（键范围分层——后续候选静脉）；不做压实
  IO 限速；不做真 IO（动作归调用方）。

## Further Notes

- 与 ChunkCompressionPolicy（块龄阈值）成压实双档按数据形态选型。
- 里程碑：14/50。
