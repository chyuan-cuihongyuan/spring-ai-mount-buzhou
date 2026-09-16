---
id: T3225
title: 回绕序号比较的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

long 序号回绕后的先后判定怎么安全化？（spec 2062 / effort #2062 / R63）

## Resolution

**TCP 回绕序号纯函数 `SequenceOrder`（core/recovery）**：compare 三态
（有符号差 b−a——溢出回绕即语义，回绕点符号仍正确；|diff| ≥ 2⁶²
安全半环即 INCOMPARABLE 诚实不臆答；等值不分先后）+isBefore/isAfter
+forwardDistance 距离折算 [−半环,+半环] 邻域（MAX→MIN 距 1）——
朴素 a<b 在回绕点反转病（MIN<MAX=true）的根治。
