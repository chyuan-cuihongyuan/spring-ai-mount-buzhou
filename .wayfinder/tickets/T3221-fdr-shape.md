---
id: T3221
title: BH-FDR 校正的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

探索性评估的假发现率怎么控制？（spec 2060 / effort #2060 / R61）

## Resolution

**BH 程序纯函数 `FalseDiscoveryRate`（core/eval）**：benjaminiHochberg
——p 升序找最大 j 使 p(j)≤(j/m)q，前 j 全显著其余不显著（截止序——
孤立小 p 不救援）；q 默认 0.05（显著集合期望假阳性占比 ≤5%）；与
Holm 配对：确证 FWER 紧 / 探索 FDR 松。
