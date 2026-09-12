# 713 — 数据集标签与过滤

> 来源：G 会话第 14 轮 = effort #713（E 会话池「数据集标签」未做项兑现）/ [T1026](../../.wayfinder/tickets/T1026-dataset-tags.md) / [T1027](../../.wayfinder/tickets/T1027-dataset-tags-verify.md) / impl 613。

## Problem

数据集数量增长后只有名字可辨（`faq-smoke` / `regression-v3` / `nightly-llm-judge`…）——「所有生产回归集」「所有 judge 系」无法一键圈出。Langfuse dataset tags 语义：评测资产的组织维度，跨项目归类/过滤。

## Solution

- `EvalDatasetMeta` 扩第 5 组件 `tags`（`List<String>`，归一化后升序不可变；4 参兼容构造默认空表——既有直构零破坏）。
- `EvalDatasetStore`：
  - `tagDataset(name, tag)` / `untagDataset(name, tag)`——read-modify-write，幂等；tag 归一 trim+lowercase，校验 `[a-z0-9:-]{1,32}`（非法 fail-fast EVAL_OPERATION_INVALID）；
  - `listDatasetsByTag(tag)`——listDatasets 过滤（同样归一后比较）；
  - metaToMap/decodeMeta 增 `tags` 字段——**旧记录无该字段解码为空表**（存量数据向后兼容，不迁移）。

## User Stories

1. 圈选：给 4 个回归集打 `regression` 标签——CI 里 `listDatasetsByTag("regression")` 一行圈定跑批范围。
2. 治理：标签即轻量元数据——dataset 用途/负责人/环境一目了然，不用挤进 name。

## Implementation Decisions

- dataset 级标签（item 级不做——条目已有 id/input/expected 结构，加标签会稀释投影语义）。
- snapshotDataset 副本**不带**原标签（快照=时点冻结，标签=组织维度——二者正交，显式重打）。
- tags 不参与 fingerprint（指纹=内容语义，标签=组织语义——刻意分离）。

## Testing Decisions

- 打标两次幂等；大小写混入归一；去标不存在标签不炸。
- listDatasetsByTag 只回命中集、按名序。
- 旧记录（手工构造无 tags 的 map JSON）解码 tags=空——兼容。
- 非法 tag（超长/坏字符/空白）fail-fast。

## Out of Scope

- item 级标签。
- 标签继承/层级。
- fingerprint 语义变更。

## Further Notes
