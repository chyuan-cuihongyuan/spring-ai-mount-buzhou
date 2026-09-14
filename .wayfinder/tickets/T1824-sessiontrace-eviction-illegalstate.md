---
id: T1824
title: OtelBridgeSink sessionTrace 驱逐 iterator.remove() 未先 next()——超限后新会话 span 静默丢弃
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

R8 分支补测实证：maxSessionTraces=1 下第三个跨会话 span 未导出（3 条记录只导出 2 条）。根因是什么、最小修复是什么？

## Resolution

**用户常设授权 AFK（可推翻）**

处置（K 会话 R8 测试显形，独立票独立 commit）：

1. **根因**：`OtelBridgeSink.sessionTrace` 驱逐块 `iterator.hasNext()` 后直接 `iterator.remove()`——CHM EntryIterator 未调用 `next()` 时 `current == null`，`remove()` 抛 IllegalStateException；该异常沿 resolveParent → openSpan/closeSpan 被 onSpan 的故障隔离 catch 吞掉（logSinkFailure 限频 WARN）。
2. **后果（潜伏缺陷）**：sessionTrace 缓存到达 `maxSessionTraces` 上限后，**每个新会话的首个 span 永远创建失败且静默消失**——驱逐护栏从未工作过（护栏本意「驱逐重建无损」，实际「驱逐即丢 span」）；对照 evictIfOverBudget（line 137–141）同款代码先 `next()` 再 `remove()`，写法正确——两处同构代码一好一坏的偏差正是覆盖率精确定位的价值。
3. **最小修复**：`iterator.remove()` 前补 `iterator.next()`（一行）；驱逐语义 = 任意条驱逐（非 LRU，Javadoc 明示），语义不变。
4. **验证**：sessionTraceEvictionRebuildsLosslessly 三会话 3 span 全导出 + traceId 重建无损断言绿；observe-otel 模块全量绿。
5. **诚实注记**：该缺陷由分支补测显形（R7 逐类分支数据精定制导的直接产物）——「缺口=未执行路径=未验证路径」方法论的有效性实证。
