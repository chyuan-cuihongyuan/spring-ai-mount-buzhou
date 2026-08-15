---
Type: task
Status: open
---
## Question

runbook 新能力条目（T97 增量）：outbox 死信处置流程、fsck 使用、索引降级语义、限幅调优、导出/导入备份恢复步骤是否齐备？

## Resolution

AFK 自决：补五节增量：§排查树增「事件丢失→查 outbox 死信」「会话列表缺失→索引未装配」两症状；§调优表增 outbox-capacity/result-limit-chars/catalog-max-entries/half-open-success-threshold；§备份恢复引用 exportSession/importSession 步骤；§告警增 buzhou.tools.result-truncated（频繁截断→工具优化）。产 impl-102。
