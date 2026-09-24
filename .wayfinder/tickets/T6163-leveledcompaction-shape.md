---
id: T6163
title: S 会话 S32 Leveled Compaction 分层压实挑选的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

压实调度怎么容量阶梯驱动且挑择确定性？（spec 5031 /
effort #5031 / S32）

## Resolution

**LeveledCompaction（core/recovery）**：LevelDB/RocksDB 层容量
阶梯思想——L0 触发线 + L_n=基准×比率^(n-1)；plan 选最高评分层
（并列浅层）最旧表为源、下一层区间重叠表（firstKey 序）为
目标；末层同层压实；complete 出账；畸形 fail-fast。
