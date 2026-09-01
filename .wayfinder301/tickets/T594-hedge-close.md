---
Type: task
Status: closed
---
## Question

装配四象限回归：默认关零 bean / yml 生效 @Primary 升位 + 端到端先回先得 /
未命中 fail-fast / 属性组非法值拒绝。

## Resolution

done（2026-09-01）：impl-324；`HedgeAssemblyTest` 五用例全绿（默认关、yml 绑定、
慢主 900ms+快冲即刻 → 冲赢、ghost 名启动红、同名自冲属性组拒绝）。
