---
id: T3163
title: 必选检查聚合的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

多检查质量门的聚合口径怎么统一原语化？（spec 2031 / effort #2031 / R32）

## Resolution

**GitHub required checks rollup 线程安全聚合 `RequiredChecksRollup`
（core/policy）**：register（required 阻断/optional 显形）+report 三态
+rollup 口径（任一必选 FAILURE 一票否决**优先于 PENDING**——已失败
不必等挂起者；任一必选 PENDING 含未报 → 门未关；全绿 SUCCESS；空集
恒开）+optionalFailures 可选退化显形+states/stats 读数。
