---
id: T6262
title: T 会话 T31 Buddy Allocator 伙伴分配器的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6261]
created: 2026-09-26
---

## Question

T31 合同怎么逐一验绿？（spec 6030 / effort #6030 / T31）

## Resolution

**验证通过**：BuddyAllocatorTest 四测全绿——16 块全分配+
全释放回归单 order4 块；分裂取低半；交错释放读数（伙伴
未整块不并）；fail-fast。
