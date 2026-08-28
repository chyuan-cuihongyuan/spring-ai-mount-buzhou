---
Type: task
Status: closed
---
## Question

AgentCostLedgerHook（afterModel usage 提取 + 定价 + agentName 净化键 + CAS 累计）+
AgentCostLedger.query()（scanByPrefix 解析 per-agent 行）。

## Resolution

done（2026-08-29）：impl-211；台账句柄覆写 CAS 透传（接口默认非原子的坑入档）。
