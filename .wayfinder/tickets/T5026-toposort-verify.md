---
id: T5026
title: Q 会话 R13 拓扑排序器的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5025]
created: 2026-09-18
---

## Question

R13 合同怎么逐一验绿？（spec 3012 / effort #3012 / R13）

## Resolution

**验证通过**：TopologicalSorterTest 八测全绿——CLRS 六点 DAG 字典
序最小 [4,5,0,2,3,1] 手算、六边前驱先于后继逐对、三角环前缀 [3]
acyclic=false、自环空序 false、无边全升序、计数读回、越界/负容量
fail-fast、随机 DAG 20 试验（20 点前向边 p=0.2）每边次序+无环恒
成立。
