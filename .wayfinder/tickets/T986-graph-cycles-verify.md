---
id: T986
title: 工具调用图谱环检测的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T985
created: 2026-09-13
---

## Question

双节点环/自环检出？无环图（链/菱形）零误报？旋转去重（A→B→A 与 B→A→B 不双报）？封顶确定？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 18 轮）：① A→B→A 检出 [A,B] 且 B→A→B 不另报（锚去重）；② 自环 A→A 检出 [A]；③ 链 A→B→C 与菱形 A→B,A→C,B→D,C→D 零误报；④ 有界：构造 >16 环图（4 节点全连双向）→ 恰 16 且截断稳定（两次调用同结果）；⑤ 空图/无边图空列表。`mvn -pl buzhou-observability -am test` 全绿。
