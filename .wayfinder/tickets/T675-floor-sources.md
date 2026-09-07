---
Type: task
Status: closed
---
## Question

SpawnAdmissionFloor 多源合成（named source、get=语义最高、default 源
兼容 335、view 观测）。

## Resolution

done（2026-09-04）：impl-365；floor 重构为 ConcurrentHashMap 源表；
四用例（多源 max/单源落回不连累/default 兼容/view）绿，335 既有
ErrorBudgetPolicy 测试零改动全绿即兼容证明。
