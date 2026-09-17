---
id: T5049
title: Q 会话 R25 Grubbs 检验的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

单点毛刺怎么统计判定不拍阈值？（spec 3024 / effort #3024 / R25）

## Resolution

**GrubbsOutlier（core/eval）**：极端学生化偏差 G=max|x−mean|/s 对
内置 α=0.05 双侧临界表（n=3..32，卡方表制先例）+矩复用
WelfordAccumulator+零方差诚实拒绝+表界 fail-fast。单离群口径
（多离群互掩归 IQR 围栏——互补声明）。
