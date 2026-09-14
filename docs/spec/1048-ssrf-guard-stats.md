# 1048 — SSRF 守卫判定分布读面

> 来源：J 会话第 48 轮 = effort #1048（[T1551](../../.wayfinder/tickets/T1551-ssrf-stats-shape.md) / [T1552](../../.wayfinder/tickets/T1552-ssrf-stats-verify.md) / impl 800）。借鉴：Fail2ban 判定链显形（拦截频次与原因分布是攻击面探测的第一信号）+ OPA decision log（判定结果按原因分桶）。与 R45 沙箱判定分轴（那轴是 fs 路径沙箱，本轴是 http 出网守卫）。
>
> **合并注记（merge resolution）**：本文件在 J 会话 R48 提交未及推送期间曾被 L 会话按收门「吸收登记」纪律忠实补档（使覆盖门死链检测转绿）；本版为两版合成——正文以 J 会话实际实现口径为准（六计数含 checks 总桶），并保留补档历史于本注记。

## Problem Statement

`SsrfGuard.check()`（spec 06 推演 #10）的全部判定路径——空主机、allowlist 精确命中、DNS 解析失败（fail-closed）、命中内网/元数据拦截段、默认放行——当前只返回 `null` 或一句拒绝字符串：**攻击面探测频次与拦截原因分布不可见**。宿主无法回答"出网校验成功率多少、拦截集中在 DNS 失败还是内网命中、allowlist 命中频次如何"；拦截段误配导致全量拒绝这类系统性故障完全静默。

## 目标

- `SsrfGuard` 增量（tools/http，静态面）：六 `AtomicLong`。
  - `checks`：check() 入口计数（总桶）；
  - `allowlisted`（allowlist 精确命中直通）/ `dnsAllowed`（DNS 解析后逐 IP 校验通过，即默认放行）两个放行桶；
  - `emptyHostRejects`（空/空白主机）/ `dnsRejects`（解析失败 fail-closed）/ `blockedRejects`（命中拦截段）三个拒绝桶。
- 嵌套 `record SsrfGuardStats(long checks, long allowlisted, long dnsAllowed, long emptyHostRejects, long dnsRejects, long blockedRejects)`（`totalAllowed()`/`totalRejects()` 派生）+ `stats()` + `resetForTest()`。
- 守恒恒等式：**checks = totalAllowed() + totalRejects()**（每入口恰落一桶）。

## 兼容性

纯增量读面：check() 返回语义（null=放行 / 非 null=拒绝原因文本）、DNS fail-closed、多解析地址任一被拦即整体拒绝逐位不变；静态面理由同 FileSandbox.stats（R45）/WriteFileStats（R46）先例；无新配置项。

## Out of Scope

- 按 host 分桶（目标主机含敏感面——红线纪律）。
- 重定向逐跳校验与 DNS rebinding 防护（开放问题维持——spec 06 推演 #10 原注）。
