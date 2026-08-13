# 07 — `run_command` 原子工具安全默认

**Impl-of:** [`.wayfinder/SPEC.md`](../SPEC.md) · **maps-to decision:** [T6](../tickets/T6-run-command-safety-default.md)

**What to build:** 让 `buzhou-tools` 的命令执行原子工具 **safe-by-default**——按 T6 决策落地（**默认关闭** `buzhou.tools.run-command.enabled=false` 显式开启才注册；或**默认开但沙箱**：命令白名单 / 容器 / 受限执行器），并与机制⑧ Hook 护栏的 HITL 联动（危险命令模式强制人工确认、不可逆操作在框架层物理走不通、授权以 state 标记放行）。引入 Buzhou 不把无限制命令执行口子暴露给模型。

**Blocked by:** 决策票 **[T6](../tickets/T6-run-command-safety-default.md)**（默认关 vs 沙箱 + HITL 联动须 grilling 拍板）。

**Status:** ready-for-agent

- [ ] T6 grilling 决策已落（默认策略 + 沙箱方案（或否）+ 与 HITL 联动）
- [ ] 安全默认生效：开箱即用时不暴露无限制命令执行
- [ ] 危险命令模式与 Hook 护栏 HITL 联动一致（tools 与 guard 安全模型统一）
- [ ] 跨平台行为（Linux / macOS / Windows）一致或有明确约束说明
- [ ] 行为变更带测试
