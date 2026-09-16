---
id: T3134
title: 布谷鸟过滤器的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3133]
created: 2026-09-17
---

## Question

CuckooFilter 合同（删除闭环/假阳性界/溢出/确定性/畸形）怎么钉住？（spec 2016 / effort #2016 / R17）

## Resolution

**八用例全绿**（首跑编译红：三目条件误用 int 判真——修 >= 0 后 8/8）：
插入查询无假阴性 / 删除闭环（离场+旁员不动+size 对账）/ 未知删除
false / 删后重插（释放后重新检疫）/ 万级非成员假阳性 <3% / 16 桶灌
200 必溢出拒插+计数 / 同序列双实例全同（确定性踢出）/ 畸形五型
（15、非 2 幂 1000、null 三入口）fail-fast。
