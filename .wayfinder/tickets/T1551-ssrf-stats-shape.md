---
id: T1551
title: SSRF 守卫判定分布读面（SsrfGuardStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1549
created: 2026-09-14
---

## Question

J 会话第 48 轮：tools/http 域的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题（跨模块批量扫描命中）：SsrfGuard.check() 全部拒绝路径（空主机 / DNS 解析失败 fail-closed / 命中内网·元数据拦截段）当前静默返回字符串——攻击面探测频次与拦截原因分布不可见，Fail2ban 判定链显形 + OPA decision log 分布思想。

形状裁决：`SsrfGuard` 内静态 `AtomicLong` 六计数——checks（入口）/ allowlisted（主机名放行清单直通）/ dnsAllowed（DNS 解析后校验通过）/ emptyHostRejects / dnsRejects / blockedRejects；嵌套 `record SsrfGuardStats`（totalAllowed/totalRejects 派生）+ `stats()` + `resetForTest()`。守恒 `checks = allowlisted + dnsAllowed + emptyHostRejects + dnsRejects + blockedRejects`。静态面理由同 FileSandbox.stats/WriteFileStats 先例（守卫实例由装配层新建）。check() 返回语义逐位不变。

Out of scope：按 host 分桶（目标主机含敏感面——红线）；重定向逐跳校验（开放问题维持，spec 06 推演 #10）。
