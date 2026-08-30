# Spec 100 — 数据集快照副本（effort #62）

> wayfinder map：`.wayfinder62/MAP.md`（T373–T374）。spec 82 fog 项收口。
> 借鉴：LangSmith dataset versioning。

## Problem Statement

指纹（spec 82）能验「同版本」，但版本本体不可冻结：数据集持续 addItem 演化，
历史 run 指向的内容漂移——A/B 或回归对比需要可长期指向的不可变版本。

## Solution

`EvalDatasetStore.snapshotDataset(source, target)`：全部条目<b>原 id 复制</b>到新
数据集（快照指纹与源一致）；target 已存在 fail-fast（快照不可覆盖——版本不可变）；
source 缺失 fail-fast；快照可继续 addItem（nextItemId 从 max+1 续排——演化分叉后
指纹自然分离）。

## User Stories

1. 作为评估作者，我要 run 前冻结版本，所以历史结论永远指向稳定内容。
2. 作为红队，我要快照指纹与源等值，所以「同版本」即刻可验。
3. 作为宿主，我要快照不可覆盖，所以版本语义不被误操作破坏。

## Implementation Decisions

- 原 id 复制（重排 id 破坏指纹等值——快照的意义即「同版本」）。
- 快照可演化（addItem 续排）——分叉是显式选择，指纹自然分离。

## Testing Decisions

- 内容/指纹/ id 与源一致；target 存在 + source 缺失双 fail-fast；
  快照 addItem 续排（000003）且指纹分叉。

## Out of Scope

- 快照血缘查询；run 绑定快照名；快照 TTL。

## Further Notes

- 组合面：`snapshotDataset("live","live-v1")` → run 指向 live-v1 → 后续 diff 的
  datasetDrift 恒 false（同指纹）——回归对比的稳定底座。
