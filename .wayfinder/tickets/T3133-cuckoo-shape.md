---
id: T3133
title: 布谷鸟过滤器的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

近似成员筛的删除语义怎么补齐？（spec 2016 / effort #2016 / R17）

## Resolution

**Cuckoo filter 线程安全过滤器 `CuckooFilter`（core/session）**：
16bit 指纹+双桶（i2=i1^hash(fp) 异或自定位）+双满轮流踢出重排
（确定性无随机——同序列同答案；MAX_KICKS=500 踢尽拒插计 overflowed）
+delete 撤销指纹槽（布伦硬缺口）+mightContain 无假阴性+size/
overflowCount 读数。与布伦互补：见过吗（不可撤）vs 还在吗（可撤）。
