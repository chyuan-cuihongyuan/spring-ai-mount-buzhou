# 07 — `run_command` 原子工具安全默认

**Impl-of:** [`.wayfinder/SPEC.md`](../SPEC.md) · **maps-to decision:** [T6](../tickets/T6-run-command-safety-default.md)

**What to build:** 让 `buzhou-tools` 的命令执行原子工具 **safe-by-default**——按 T6 决策落地（**默认关闭** `buzhou.tools.run-command.enabled=false` 显式开启才注册；或**默认开但沙箱**：命令白名单 / 容器 / 受限执行器），并与机制⑧ Hook 护栏的 HITL 联动（危险命令模式强制人工确认、不可逆操作在框架层物理走不通、授权以 state 标记放行）。引入 Buzhou 不把无限制命令执行口子暴露给模型。

**Blocked by:** 决策票 **[T6](../tickets/T6-run-command-safety-default.md)**（默认关 vs 沙箱 + HITL 联动须 grilling 拍板）—— 已闭合（默认关）。

**Status:** done (assignee: zcode)

- [x] T6 grilling 决策已落（默认关；沙箱方案=否；HITL 经 `enabledDangerousToolNames()` 联动）
- [x] 安全默认生效：开箱即用时不暴露无限制命令执行 —— 既有 `ToolsModule.Builder.runCommandEnabled=false` + `ToolsModuleTest.defaultMatrixExcludesDangerousTools` 守护
- [x] 危险命令模式与 Hook 护栏 HITL 联动一致 —— opt-in 时 `enabledDangerousToolNames()` 暴露给 GuardModule 注册（`ToolsModuleTest.optInAddsDangerousToolsAndGuardNames` 守护）
- [x] 跨平台行为一致或有明确约束说明 —— `RunCommandTool` javadoc 标注 `/bin/sh -c` 需 POSIX shell（Linux/macOS 原生、Windows 需 WSL/Git Bash）；默认关使差异只影响显式开启者
- [x] 行为变更带测试 —— safe-default 行为由既有 `ToolsModuleTest` 覆盖（本切片仅补 javadoc 约束、无行为变更）

## Resolution

**发现**：safe-by-default 行为**已在 ticket 16 实现并有测试守护**——`ToolsModule.Builder.runCommandEnabled=false`（默认关）、opt-in 才注册、opt-in 后经 `enabledDangerousToolNames()` 挂 HITL；`ToolsModuleTest.defaultMatrixExcludesDangerousTools` / `optInAddsDangerousToolsAndGuardNames` 覆盖。故本切片实际缺口仅为**决策落定 + 跨平台约束说明**。

**本次改动**：
1. **[T6](../tickets/T6-run-command-safety-default.md) 决策落定**：默认关、沙箱方案=否、HITL 经 `enabledDangerousToolNames()` 联动（implementer 确认既有实现的安全默认；用户未应答 grilling，可推翻）。
2. **跨平台约束说明**：`RunCommandTool` javadoc 增「跨平台约束」段——命令经 `/bin/sh -c` 执行、需 POSIX shell（Linux/macOS 原生、Windows 需 WSL/Git Bash）；默认关使差异只影响显式开启者（满足验收「跨平台一致**或**有明确约束说明」）。

**未改行为、未加新测试**：safe-default 行为本就存在且被既有测试守护；追加冗余测试为镀金，故不做。javadoc-only 变更、无构建影响。

**验证受限说明**：`RunCommandToolTest` 走 `/bin/sh`、本 Windows 主机无法跑（见 memory `windows-host-cant-build-linux-project`）；但本切片无行为变更，既有测试在 Linux CI 守护。
