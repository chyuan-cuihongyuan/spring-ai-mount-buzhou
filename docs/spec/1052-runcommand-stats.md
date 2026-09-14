# 1052 — run_command 执行结果分布读面

> 来源：J 会话第 52 轮 = effort #1052（[T1559](../../.wayfinder/tickets/T1559-runcommand-stats-shape.md) / [T1560](../../.wayfinder/tickets/T1560-runcommand-stats-verify.md) / impl 804）。借鉴：Kubernetes Job status（按结局分桶）。command 域第二轴收官：R51 黑名单判定 / R52 执行本身。

## Problem Statement

`RunCommandTool.call()`（spec 06 / impl-49 硬化）的全部结局——正常退出（含非零 exit）、取消、超时终止、四类参数拒绝、异常兜底——当前只返回字符串：**执行成功率与结局分布不可见**。宿主无法回答"命令执行成功率多少、失败集中在模型参数错误（空命令/坏 workdir/坏 timeout）还是运行时（超时/取消）"；黑名单高频命中（模型反复试探）在 R51 判定面可见，但执行面自身无对照。

## 目标

- `RunCommandTool` 增量（tools/command，静态面）：九 `AtomicLong`。
  - `attempts`：call() 入口（总桶）；`exits`：正常退出（含非零 exit——进程送达即入桶）；`canceled`（取消）；`timeouts`（超时终止）；
  - `blankRejects` / `blacklistRejects` / `workdirRejects` / `timeoutParamRejects` / `failures`（catch 兜底）五个拒绝桶。
- 嵌套 `record RunCommandStats(...)`（`totalRejects()` = 四参数桶 + failures 派生）+ `stats()` + `resetForTest()`。
- 守恒恒等式：**attempts = exits + canceled + timeouts + totalRejects()**。

## 兼容性

纯增量读面：call() 返回语义、黑名单/超时/进程树终止语义逐位不变；静态面理由同 R46–R51 先例；无新配置项。

## Out of Scope

- exit code 分布直方图（exits 单桶已回答送达率）。
- SandboxRunCommandTool 独立计量（装饰同族共享底座读面）。
