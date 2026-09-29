# Spec 8003 — LevenshteinAutomaton 编辑距离自动机（effort #8003，V4）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8007–V8008，impl 2305）。
> 借鉴：Schulz & Mihov 2002（Lucene 模糊查询同源思想）。

## Problem Statement

模糊匹配的病：对词典逐词全矩阵算 Levenshtein——**模式
固定而候选海量时，模式侧的自动机可以只按活性状态行
推进**，且把「距离 ≤ k」判定与距离计算耦合全算（浪费）。

## Solution

`LevenshteinAutomaton`（core/metrics）：`of(pattern, maxEdits)`
构建；`matches(word)` 逐字符推进活性状态行（位置 0..m 的
编辑数向量，min(上+1/左+1/左上+cost) 三源取小），终态值
≤ maxEdits 即接受——自动机按候选流式推进、早停可行（行
最小值 > maxEdits 时后续只会更大，诚实缺省 false）；null/
空模式/负 k fail-fast。

## Testing Decisions

- 手锚（kitten/2：kitchen=2 接受、sitting=3 拒绝；空词距离
  =模式长）；300 随机 vs 全矩阵 DP 圣像判定全等；早停单调
  性钉住；fail-fast。

## Out of Scope

- 不做 DFA 离线确定化；不做换位（Damerau）扩展。

## Further Notes

- 与 DamerauLevenshtein（7022）同族不同面：四算子全矩阵
  距离值 vs 模式侧 k-容差接受判定（流式推进）。
- 里程碑：V4/50（8%）。
