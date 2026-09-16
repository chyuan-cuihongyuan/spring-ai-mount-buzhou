---
id: T3226
title: 回绕序号比较的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3225]
created: 2026-09-17
---

## Question

SequenceOrder 合同（普通/回绕点/半环界/距离/畸形）怎么钉住？（spec 2062 / effort #2062 / R63）

## Resolution

**六用例全绿**（首跑 1 红教训：病证断言自己写反——MAX<MIN 本就
false，病证正确表述是 MIN<MAX=true 把新者判前；修后 6/6）：普通序
双向 / 回绕点病证对照（朴素 true vs 安全 false）+邻域小步 / 半环界
内 BEFORE 外 INCOMPARABLE / 等值 INCOMPARABLE / 距离折算（回绕距 1、
哨兵）/ 反向回绕。
