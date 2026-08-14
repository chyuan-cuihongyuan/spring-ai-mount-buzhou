---
id: T58
title: core · 租约续租、fence 与 LeaseLost 语义
type: grilling
status: closed
assignee: ""
blocked-by: [T55]
created: 2026-08-14
---

## Question

长 Turn 期间租约静默过期导致双主的风险如何消除？需裁决：① 自动续租触发点（工具轮间隙？后台 scheduled？）与续租节奏（TTL 的几分之一）；② renew 失败（被 steal）时 LeaseLostException 的抛出路径与 Turn 中止语义（在途工具结果是否入历史）；③ fencingToken 写路径检查的接入面（哪些 store 写前校验）；④ InMemorySessionLeaseStore 过期租约物理移除；⑤ lease TTL/续租间隔全部可配化。

## Resolution

**ratify（用户常设授权，可推翻）→ spec 13 §core-3**：续租双路径（轮间 renew + 后台 TTL/3 调度）；renew 失败 → LeaseLostException → Turn 中止（在飞丢弃、不入 Completed-Turn——双主窗口本地零写入）；Turn 提交点校验 fencingToken（写路径 fence）；InMemory 过期租约物理移除；TTL/续租间隔可配（HikariCP maxLifetime 抖动防同步思想吸收进续租节奏）。
