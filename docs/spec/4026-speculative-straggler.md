# Spec 4026 — 推测执行裁决（effort #4026，R27）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6053–T6054，impl 2127）。
> 借鉴：Spark speculative execution（慢者复制竞争）。

## Problem Statement

尾部等待被长尾任务的**机器性慢**（非数据性慢）拖死——全阶段
等一个 straggler；副本竞争的裁决件缺失。

## Solution

`SpeculativeStragglerPolicy`（core/exec，纯裁决）：

- 显著慢：elapsed > 同伴中位 ×multiplier（中位抗离群——均值被
  异常完成者拉偏）；进度落后：progress < threshold（默认 0.75
  级——快完成的慢任务不折腾，副本大概率赶不上白烧资源）；
- medianElapsed 偶数取下中位（确定性）；TaskStats(id,elapsed,
  progress) 读数面。

## User Stories

1. 作为批作者，机器性 straggler 被副本竞争兜住——尾部不拖死。
2. 作为成本作者，进度过阈豁免——资源不白烧。

## Testing Decisions

- 400>200×1.5 且 0.4<0.75 → 推测；慢但 0.8 过阈豁免；290 不够
  慢与 300 恰界不含双豁免；中位抗离群（60s 完成者不拉偏）+
  偶数下中位；畸形七型 fail-fast。

## Out of Scope

- 不做真副本调度/先到先胜收敛（归执行器）；不做 per-stage 动态
  阈值；不做 kill 原任务语义。

## Further Notes

- 与 Hedged requests（网络对冲）同思想不同面：请求级延迟对冲
  vs 任务级进度中位对比。
- 里程碑：27/50。
