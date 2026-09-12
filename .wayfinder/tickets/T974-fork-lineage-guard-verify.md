---
id: T974
title: fork 谱系游走环防护的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T973
created: 2026-09-13
---

## Question

正常树游走到根即停？环检测真断（A→B→A）？深度封顶生效？缺源会话诚实空谱系？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 12 轮）：① 三层树（根←中←叶）walk 叶——ancestors=[中,根] 有序、两标志 false；② 构造 A↔B 环——loopDetected=true 且游走终止（不 OOM）；③ maxDepth=2 的 5 层链——depthCapped=true；④ 无 SOURCE 会话——空 ancestors；⑤ 键统一走 SessionForkKeys.SOURCE（漂移即断的既有纪律）。`mvn -pl buzhou-core -am test` 全绿。
