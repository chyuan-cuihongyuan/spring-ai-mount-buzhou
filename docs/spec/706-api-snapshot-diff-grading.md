# 706 — API 快照 diff 破坏性分级

> 来源：G 会话第 7 轮 = effort #706（借鉴 oasdiff / OpenAPI diff breaking-change grading）/ [T963](../../.wayfinder/tickets/T963-snapshot-diff-grade-shape.md) / [T964](../../.wayfinder/tickets/T964-snapshot-diff-grade-verify.md) / impl 509。

## 背景

快照门（spec 615）按集合全等一刀切失败——「新增类型未入档」（正常迭代，regenerate 即可）与「公共类型消失」（破坏性，须审查 0.x 语义与 api-surface.md）在失败信息里不可区分，处置成本错配。

## 目标

- `SnapshotDiff`（快照测试内纯函数分类器）：`gradeDiff(expected, actual)` → added/removed 有序清单 + `breaking()`（只看 removed）+ `gradeMessage()` 分级报告。
- 门语义不变：任何 diff 仍失败（不给静默漂移留门）；升级的是失败信息——分级计数、破坏性清单前置、分类处置指引（added → regenerate + api-surface.md 同步；removed → 审查 0.x 语义）。

## 非目标

不做「容忍非破坏 diff」选项（快照随轮再生的既有纪律不变）；不扫类型成员级签名（面口径仍为 public 类型全集）。

## 测试

纯 added / 纯 removed / 混合三类 diff 分级正确；breaking 只由 removed 驱动；既有门行为（classpath 假设门 + regenerate 门控）不变。

## 兼容性

测试域纯增量；生产源码零变化。
