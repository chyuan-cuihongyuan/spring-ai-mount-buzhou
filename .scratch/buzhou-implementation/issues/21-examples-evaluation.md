# 21 — examples 排障 demo 与评测

**What to build:** 运维排障 Agent 单场景多脚本：记忆压缩链/可观测回放/护栏与 HITL/Skill+MCP 热插拔四簇脚本，mock DB+HTTP 设施；评测脚本独立目录（复用 mock）：四指标（P0 保留率/续接成功率/事实召回/压缩率）+20 轮端到端两级联动用例，接入 CI 作回归门禁；README quickstart 指向 demo。

**Blocked by:** 13, 14, 15, 16, 17

**Status:** ready-for-agent

- [ ] 排障 demo 一条命令跑通四簇脚本（README 可复现）
- [ ] 评测四指标出报告，阈值回归挂 CI
- [ ] demo 覆盖：Spill 触发与回读、HITL 确认往返、Skill 加载、MCP 热更、压缩后 P0 锚定
- [ ] 评测脚本与 demo 分目录、共享 mock 设施
