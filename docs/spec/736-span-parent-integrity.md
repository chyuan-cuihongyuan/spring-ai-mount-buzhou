# 736 — span 父链完整性审计

> 来源：G 会话第 37 轮 = effort #736（OTel trace 树语义；观测族三层收口）/ [T1072](../../.wayfinder/tickets/T1072-span-parent-integrity.md) / [T1073](../../.wayfinder/tickets/T1073-span-parent-integrity-verify.md) / impl 636。

## Problem

span 以 parentSpanId 构成 trace 树——父 span 被容量逐出（729）、未落库、或跨 trace 串写时，子 span 变成「悬空节点」：trace 树断裂，TurnReplay 重放与依赖分析失真。悬空是静默的（查询按 spanId 单点查不查父链）。

## Solution

`SpanParentIntegrityAudit.audit(List<SpanRecord>)` 纯函数：集合内 spanId 索引 + 悬空父引用发现（Finding(spanId, parentSpanId)，按 spanId 字典序）+totalSpans/rootSpans 计数。无父=根合法。调用方供单会话或单 trace 集合（跨集合引用本就不合法）。

## Out of Scope

环检测（记录不可变、parent 先于 child 产生——环不构成）；自动修复（归 729 逐出策略）。
