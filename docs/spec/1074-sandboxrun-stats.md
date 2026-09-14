# 1074 — 沙箱版 run_command 执行分布读面

> 来源：J 会话第 74 轮 = effort #1074（[T1603](../../.wayfinder/tickets/T1603-sandboxrun-stats-shape.md) / [T1604](../../.wayfinder/tickets/T1604-sandboxrun-stats-verify.md) / impl 826）。借鉴：同 R52 Kubernetes Job status（按结局分桶）。command 域第三轴收官（R51 黑名单/R52 直执行/R74 沙箱档）。

## Problem Statement

`SandboxRunCommandTool.call()`（沙箱档独立实现：SAFE_WORKDIR_SEGMENT 校验 + 黑名单 + launcher 委派）六路径零计数——**沙箱档执行成功率与拒绝原因分布不可见**；尤其「workdir 非法路径段拒绝」（SAFE_WORKDIR_SEGMENT 独有防线）的触发频次是模型路径试探的直接信号。

## 目标

- `SandboxRunCommandTool` 增量（tools/command，静态面）：七 `AtomicLong`。
  - `calls`：call 入口（总桶）；`runs`：dispatch 正常返回（送达）；
  - `blankRejects` / `blacklistRejects` / `workdirRejects`（不存在与非法段合并桶）/ `timeoutParamRejects` / `failures`（catch 兜底）五个拒绝桶。
- 嵌套 `record SandboxRunStats(...)` + `stats()` + `resetForTest()`。
- 守恒恒等式：**calls = runs + 五拒绝桶之和**（每入口恰落一桶）。

## 兼容性

纯增量读面：call() 返回语义、SAFE_WORKDIR_SEGMENT 校验与 launcher 委派逐位不变；静态面理由同 R46–R73 先例；无新配置项。

## Out of Scope

- launcher 内部取消/超时明细（dispatch 抽象层语义）。
- 按命令分桶（敏感面红线）。
