# 1067 — 会话 Span 树拓扑读面

**What to build:** SpanTreeTopology 纯函数（analyze→Topology 深度/扇出/根/孤儿计数+kind 直方+环防护）+ 五测。

**Blocked by:** None.

**Status:** done

- [x] SpanTreeTopology（core/observability，迭代式深度+visited 环防护）
- [x] SpanTreeTopologyTest（哨兵/三层树/孤儿/环/多根典序）
- [x] spec 1414 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='SpanTreeTopologyTest'` 5/5 绿。
