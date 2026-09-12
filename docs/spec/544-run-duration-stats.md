# Spec 544 — 评估 run 项耗时分布（effort #544）

> wayfinder map：`.wayfinder/maps/effort-544.md`（T845-846）。E 会话第 44 轮。

## Problem Statement

run 总时长 timer（111）+ 项 durationMs 字段在——run 内「慢在哪一项」
的分布读数空白。

## Solution

`eval.EvalRunDurationStats`（纯函数，416 分位族同法）：analyze(run) →
DurationStats(items, p50, p95, max, slowest top3)——exact 最近秩；空
run null 诚实空值；null run fail-fast；最慢 top3 降序+同值字典序稳定。

## User Stories

1. 作为评测方，我想一眼看到 run 慢在哪一项， so 长尾项（死循环 judge/
   超长输出）秒级定位。

## Implementation Decisions

- 纯函数读数（跑内分布；跨 run 趋势归 JSONL 下游）。

## Testing Decisions

- 分位数学断言；最慢 top3 降序；空 run null；null fail-fast。

## Out of Scope

- 跨 run 趋势；JSONL 导出。

## Further Notes

- 新公共类型 `EvalRunDurationStats`（嵌套 `DurationStats`/`SlowestItem`）
  随轮 regenerate 快照 + api-surface.md 加行。
