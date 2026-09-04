---
Type: task
Status: closed
---
## Question

`AlertGate` 通知策略门（core/health）：静默窗状态机（机制匹配 + 到期 +
惰性过期 + 运行时按钮）与抑制判定（自观察 firing 视图，根因遮蔽衍生），
被吞/被抑留痕（WARN + 计数器）。

## Resolution

done（2026-09-04）：impl-353；`AlertGate` 落地（Silence/InhibitRecord/
Silenced 观测 record；观察先于判定——被抑 FIRING 仍进 firing 视图，源
恢复后目标新触发可再通知；静默优先报告；过期惰性清理）。十二用例绿
（含伪时钟过期/恢复同吞/抑制解除再通知/`*` 全量匹配）。
