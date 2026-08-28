# Spec 81 — run 对比 diff（effort #42）

> wayfinder map：`.wayfinder42/MAP.md`（T317–T318）。借鉴：LangSmith run compare /
> Promptfoo trend diff。

## Problem Statement

历史 run 已落盘可回读（spec 52/74/76），但无对比面：改 prompt/模型后「哪些项由绿
变红、哪些修复」需要宿主拉两份记录手工对——回归分析成本高且口径各家漂移。

## Solution

`EvalRunDiff.diff(base, head)` 纯函数：同 itemId 对齐状态迁移，四态分类
（REGRESSION 绿变红 / FIX 红变绿 / STABLE_PASS / STABLE_FAIL——红态内部 fail↔error
不细分）；单侧项（BASE_ONLY/HEAD_ONLY——两 run 间数据集漂移）单独计数不进四态；
`netDelta() = fixes - regressions`；项序确定（base 序优先、head 新增随后）。
`runOf(runId, dataset, itemId:status...)` 便捷构造（测试/宿主拼装面）。

## User Stories

1. 作为评估作者，我要一眼看出回归项，所以 prompt 改动的代价可审。
2. 作为质量负责人，我要数据集漂移显形，所以单侧项不污染回归计数。
3. 作为 CI 作者，我要 netDelta 一个数，所以趋势门可判。

## Implementation Decisions

- 纯函数（不触 store——输入从 EvalQueryService.run/abRun 回读）。
- 红/绿二分（fail↔error 都是红——对比语义关心红绿不关心理由）。

## Testing Decisions

- 四态混合序列逐项断言（含 error→pass 计 FIX、fail→error 计 STABLE_FAIL）；
- 单侧项计数与四态隔离；runOf 计数一致；项序确定。

## Out of Scope

- diff 落盘/事件；A/B 明细 diff；多 run 趋势序列。

## Further Notes

- 与 EvalGate（spec 80）互补：gate 判单 run 水位，diff 判两 run 迁移。
