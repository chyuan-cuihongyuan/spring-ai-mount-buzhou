---
Type: task
Status: closed
---
## Question

观测导出尾采样：错误/慢全留 + 确定性哈希留样 + 会话粒度。

## Resolution

done（2026-08-30）：impl-280；`exportAllSampled` + `TailSamplingPolicy` +
`SampledExportResult`（kept/notSampled/degraded 分列）+ 红队 5 例 + 导出器回归。
