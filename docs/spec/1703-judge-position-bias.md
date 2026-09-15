# Spec 1703 — 裁判位置偏差读面（effort #1703，R4）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2607–T2608，impl 1303）。借鉴：
> MT-Bench / FastChat（~37K★）的裁判位置偏差检验——LLM 裁判偏爱先出现的
> 回答，「换位不服」即偏差，须显形而非默许。

## Problem Statement

成对裁判（PairwiseJudge）对 (A,B) 与换位 (B,A) 的裁决若不镜像一致，说明
裁判在吃位置而非内容——JudgeAgreement 只量「两裁判一致度」，**同裁判的
位置自洽度**缺位，偏差裁判在给门喂毒分数。

## Solution

`JudgePositionBias`（core/eval，静态纯函数）：

- 输入 `PairJudgement(id, verdictAB, verdictBA)`（`Verdict` 闭集 A/B/TIE）；
- `analyze(judgements)` → `BiasReport(pairs, consistent, firstWinsBoth,
  secondWinsBoth, mixedTie, biasRatio)`：
  - 镜像一致：AB=A&BA=B / AB=B&BA=A / 双 TIE；
  - 位置偏差：首位双赢（AB=A&BA=A）/ 次位双赢（AB=B&BA=B）；
  - 混合平：恰一次 TIE；biasRatio = 双赢和/pairs（pairs=0 哨兵 −1）。

## User Stories

1. 作为评测维护者，biasRatio=0.42 显形裁判偏袒首位——换位复裁或换裁判。
2. 作为审计者，firstWinsBoth/secondWinsBoth 分辨偏 first 还是偏 second。
3. 作为框架宿主，任何成对裁决记录（含导出回放）零适配入口。

## Implementation Decisions

- 纯读面不改 PairwiseJudge；裁决域闭集三值（不猜裁判内部分数）。
- 双 TIE 记一致（裁判两次都说平——自洽）；单次 TIE 记 mixedTie（不稳定信号）。

## Testing Decisions

- 空表哨兵 −1；三类镜像一致各一例全入 consistent；
- 首位/次位双赢与混合平分桶正确，biasRatio=0.5 断言；
- null 按空表。

## Out of Scope

- 不做裁判间一致度（归 JudgeAgreement）；不做位置换权重/置信度维。

## Further Notes

- MT-Bench 结论：GPT-4 裁判首位偏好显著——位置偏差读面是 LLM-as-judge
  的卫生底线，与 JudgeCalibration（校准）互补。
