---
id: T1026
title: 数据集标签与过滤的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-12
---

## Question

数据集只有名字可辨——加标签维度吗？item 级做不做？snapshot 带不带标签？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 14 轮 = effort #713 / spec 713 / impl 613）：EvalDatasetMeta 扩 tags 组件（归一升序不可变+4 参兼容构造）；store tagDataset/untagDataset 幂等+归一 `[a-z0-9:-]{1,32}` fail-fast+listDatasetsByTag 过滤；meta 编解码增 tags 字段旧记录解码=空表（零迁移）。item 级不做；snapshot 副本不带标签（正交显式重打）；tags 不入 fingerprint。
