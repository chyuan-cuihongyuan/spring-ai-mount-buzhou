# Spec 136 — 观测导出尾采样（effort #113）

> wayfinder map：`.wayfinder113/MAP.md`（T461–T462）。借鉴：OpenTelemetry
> tail sampling（尾部采样在出口裁决，保错误与慢样本）。

## Problem Statement

观测 JSONL 全量导出在规模部署下体积线性膨胀；头部采样（ingress 随机丢）会
丢掉恰恰最需要的错误会话——排障黄金样本和健康噪音同权。

## Solution

`ObservabilityJsonlExporter.exportAllSampled(out, policy)`：会话粒度尾采样。
`TailSamplingPolicy(baseRatePercent, slowThreshold)`——错误会话（任一 span
status=ERROR）与慢会话（任一 span 时长 > 阈值）**100% 保留**；其余按
`hash(sessionId) % 100 < rate` **确定性**采样（同 id 重导同判定——增量
at-least-once 语义下幂等一致）。行管线与 exportAll 同构（行内容逐字节一致）；
`SampledExportResult` kept/notSampled/spans/events/degraded 分列 + 单行 summary；
rate=0 = 只留错误+慢的最小面。

## User Stories

1. 作为 SRE，导出体积可控的同时错误与慢会话一条不丢，所以排障黄金样本永远
   在库里。
2. 作为数据工程，确定性采样让同会话重导判定一致，所以 OLAP 端 upsert 幂等。

## Testing Decisions

- 红队：错误+慢保留 / rate=0 丢健康（行面断言不含健康会话）；确定性（两次导出
  同判定同输出）；空库诚实零行；rate=100 全留；策略参数 fail-fast。既有导出器
  测试回归。

## Out of Scope

- 按错误签名分层采样率；span 粒度；热调率；gzip 组合面（调用方可自行包流）。

## Further Notes

- 与 spec 109/119 gzip 族正交组合：`exportAllSampled` 后接 GZIPOutputStream 即得
  「尾采样 + 压缩」双降。
