# Wayfinder Map — Buzhou 观测导出尾采样（effort #113，A 会话第 8 轮）

> A 侧编号策略沿用 #111 声明。借鉴 OpenTelemetry tail sampling。

## Destination

导出体积治理：错误/慢会话 100% 保留、健康快会话按确定性哈希比率留样——
会话粒度完整叙事不丢，OLAP 端体积可控。

## Notes

- 会话粒度而非 span 粒度（保完整叙事）；确定性采样 = 同 id 重导同判定
  （at-least-once 幂等一致）；rate=0 即「只留错误+慢」最小面。

## Decisions so far

- [尾采样导出](../tickets/T461-tail-sampling.md) — exportAllSampled +
  TailSamplingPolicy(rate, slowThreshold) + SampledExportResult 单行 summary。

## Not yet specified

- 按错误签名分层采样率（同族错误只留 N 条会话——与 spec 83 组合）。

## Out of scope

- span 粒度采样；跨会话 trace 采样；远端配置热调率。

## Tickets

- [x] [T461 尾采样导出](../tickets/T461-tail-sampling.md)（impl-280）
- [x] [T462 收口提交](../tickets/T462-tail-sampling-close.md)（impl-280）
