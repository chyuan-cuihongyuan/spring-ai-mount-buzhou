---
id: T6022
title: R 会话 R11 UUIDv7 的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6021]
created: 2026-09-23
---

## Question

R11 合同怎么逐一验绿？（spec 4010 / effort #4010 / R11）

## Resolution

**验证通过**：UuidV7MonotonicTest 五测全绿——百次生成 ver=7/
variant=2/时间戳回读；固定钟 200 个严格递增全唯一（字典序=生成序）；
5 千生成（>4096 计数器容量）借位后仍严格有序、伪时序幅度 ≤2ms；
时钟推进回读 + 计数器 0..0xFFF 界内；畸形五型 fail-fast（null×2/
null id/v4 拒判）。
