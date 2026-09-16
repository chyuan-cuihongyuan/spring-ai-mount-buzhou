---
id: T3204
title: 加权无放回抽样的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3203]
created: 2026-09-17
---

## Question

WeightedSample 合同（无重复/零权/截断/回放/主导/畸形）怎么钉住？（spec 2051 / effort #2051 / R52）

## Resolution

**七用例全绿**（两轮编译红修复：局部泛型 record 裸方法引用推断失败
改显式 lambda、lambda 参数名 k 与方法参数遮蔽冲突改名；修后 7/7）：
50 轮 3/5 无重复 / 零权 100 轮永不中 / k=10 超池恰全合格（零权除外）
/ k=0 空 / 同种 42 双跑同序列 / 100:1 权重千轮 heavy>950 / 畸形五型
（null×2、负 k、负权、NaN 权）fail-fast。
