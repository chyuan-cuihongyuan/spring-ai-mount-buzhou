---
id: T3158
title: 并发组闸的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3157]
created: 2026-09-17
---

## Question

ConcurrencyGroupGate 合同（互斥/取代/栅栏/计数/畸形）怎么钉住？（spec 2028 / effort #2028 / R29）

## Resolution

**八用例一次全绿**（buzhou-core）：空闲授予+ownerOf / 重入幂等零拒 /
占用拒 owner 不动+计数 / 取代接管+被取代者迟到 complete 栅栏拦下
不误伤新属主 / 正常完成释放后可再入 / 空闲组 complete false / 组间
独立互不干扰 / 畸形六型（null/blank group、null taskId ×3、null
ownerOf）fail-fast。
