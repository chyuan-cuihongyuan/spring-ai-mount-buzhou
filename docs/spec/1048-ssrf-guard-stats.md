# 1048 — SSRF 守卫判定分布读面

> 来源：J 会话第 48 轮 = effort #1048（[T1551](../../.wayfinder/tickets/T1551-ssrf-stats-shape.md) / [T1552](../../.wayfinder/tickets/T1552-ssrf-stats-verify.md) / impl 800）。借鉴：Fail2ban 判定链显形（拦截频次与原因分布是攻击面探测的第一信号）+ OPA decision log（判定结果按原因分桶）。与 R45 沙箱判定分轴（那轴是 fs 路径沙箱，本轴是 http 出网守卫）。

## Problem Statement

`SsrfGuard.check()`（spec 06 推演 #10）的全部拒绝路径——空主机、DNS 解析失败（fail-closed）、命中内网/元数据拦截段——当前只返回拒绝字符串：**攻击面探测频次与拦截原因分布不可见**。宿主无法回答"出网校验成功率多少、拦截集中在 DNS 失败还是内网命中"；系统性探测（大量内网段命中）与偶发笔误（DNS 失败）无法区分。

## 目标

- `SsrfGuard` 增量（tools/http，静态面）：六 `AtomicLong`。
  - `checks`：check() 入口计数（总桶）；
  - `allowlisted`（主机名放行清单直通）/ `dnsAllowed`（DNS 解析后逐 IP 校验通过）两个放行桶；
  - `emptyHostRejects` / `dnsRejects`（解析失败 fail-closed）/ `blockedRejects`（命中拦截段）三个拒绝桶。
- 嵌套 `record SsrfGuardStats(long checks, long allowlisted, long dnsAllowed, long emptyHostRejects, long dnsRejects, long blockedRejects)`（`totalAllowed()`/`totalRejects()` 派生）+ `stats()` + `resetForTest()`。
- 守恒恒等式：**checks = totalAllowed() + totalRejects()**（每入口恰落一桶）。

## 兼容性

纯增量读面：check() 返回语义、放行清单/拦截段判定逐位不变；静态面理由同 FileSandbox.stats（R45）/WriteFileStats（R46）先例；无新配置项。

## Out of Scope

- 按 host 分桶（目标主机含敏感面——红线纪律）。
- 重定向逐跳校验与 DNS rebinding 防护（开放问题维持——spec 06 推演 #10 原注）。
