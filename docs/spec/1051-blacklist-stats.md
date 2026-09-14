# 1051 — 命令黑名单拦截判定读面

> 来源：J 会话第 51 轮 = effort #1051（[T1557](../../.wayfinder/tickets/T1557-blacklist-stats-shape.md) / [T1558](../../.wayfinder/tickets/T1558-blacklist-stats-verify.md) / impl 803）。借鉴：Fail2ban 规则命中计数（拦截规则自身的命中频次是规则有效性与试探行为的第一信号）。与 R48 SSRF 守卫分轴（那轴是出网 IP 判定，本轴是命令文本判定）。

## Problem Statement

`CommandBlacklist.matches()`（spec 06 run_command 守门）是纯布尔静默判定：**黑名单命中频次与放行比完全不可见**。宿主无法回答"模型在多大比例的调用里试探危险命令、命中是集中于个别模式还是弥散"；黑名单误配（过宽模式全量拦截）导致工具事实上不可用这类系统性故障也完全静默。

## 目标

- `CommandBlacklist` 增量（tools/command，静态面）：三 `AtomicLong`。
  - `checks`：matches() 入口计数；
  - `matched`：true 返回（拦截）；
  - `allowed`：false 返回（放行，含 null/空白短路路径——语义即未拦截，口径诚实不设假面）。
- 嵌套 `record CommandBlacklistStats(long checks, long matched, long allowed)` + `stats()` + `resetForTest()`。
- 守恒恒等式：**checks = matched + allowed**（每入口恰落一桶）。

## 兼容性

纯增量读面：matches() 返回值、模式编译与大小写不敏感匹配逐位不变；静态面理由同 R46–R49 先例；无新配置项。

## Out of Scope

- 按 pattern 分桶（模式清单是配置面，命中分布留后续轮按需）。
- RunCommandTool / SandboxRunCommandTool 执行结果分布（另轮分轴）。
