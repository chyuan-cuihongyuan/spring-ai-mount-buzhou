# 1048 — SSRF 守卫判定分布读面

> **吸收补档（2026-09-14，L 会话收门时发现）**：本 spec 为 J 会话 R48 的前置登记号——README 生产级纵深表已先行登记该行（「出网校验放行/拒绝按原因分桶显形，五桶守恒」），但 spec 文件未及随轮提交，覆盖门死链检测拦截。按 H 会话收口「吸收登记」纪律由 L 会话按已登记描述忠实补档；实现归 J 会话 R48（票 T1551–T1552 / impl 800，effort-1000.md 台账计划行）。

## Problem Statement

`SsrfGuard.check(host)`（tools/http，spec 76 / impl-49 SSRF 硬化：fe80::/10 链路本地段等）的五条判定路径——空主机、allowlist 精确命中、DNS 解析失败（fail-closed）、命中内网/元数据拦截段、默认放行——当前只返回 `null` 或一句拒绝字符串：**出网校验的放行/拒绝分布不可见**。宿主无法回答「出网成功率多少、拒绝集中在哪类原因、allowlist 命中频次如何」；拦截段误配导致全量拒绝这类系统性故障完全静默。

## 目标

- `SsrfGuard` 增量（静态面，理由同 FileSandbox.stats 先例——实例由装配层新建）：五 `AtomicLong` 分桶。
  - `allowedDefault`：默认放行（解析全通过、不命中任何段）；
  - `allowedAllowlist`：allowlist 精确命中放行；
  - `rejectedBlank`：空/空白主机拒绝；
  - `rejectedDnsFailure`：DNS 解析失败按拒绝处理（fail-closed 桶）；
  - `rejectedBlocked`：命中内网/元数据拦截段拒绝。
- 嵌套 `record SsrfGuardStats(...)` + `stats()` 只读快照 + `resetForTest()`。
- 守恒恒等式：**checks = allowedDefault + allowedAllowlist + rejectedBlank + rejectedDnsFailure + rejectedBlocked**（每入口恰落一桶）。

## 兼容性

纯增量读面：`check()` 返回语义（null=放行 / 非 null=拒绝原因文本）、DNS fail-closed、多解析地址任一被拦即整体拒绝的语义逐位不变；无新配置项。

## Out of Scope

- 按主机名分桶（主机含敏感面——红线纪律）。
- 拦截段清单变更审计（另轴）。
- http_request 侧的响应计量（J R47 沙箱判定分轴已覆盖工具执行侧）。
