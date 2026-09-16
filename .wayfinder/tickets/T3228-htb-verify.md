---
id: T3228
title: 层级令牌桶的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3227]
created: 2026-09-17
---

## Question

HierarchicalTokenBucket 合同（双闸/硬顶/自限/封顶/畸形）怎么钉住？（spec 2063 / effort #2063 / R64）

## Resolution

**六用例全绿**（三轮修复教训：Map.of 整数字面量推断 Integer 须显式
Double ×2、父余量断言忘扣账（50+30=80 非 30）；修后 6/6）：双扣对账
（父 70/子 20/旁桶 50）/ 子合 200 父 100 父尽后 b 拒且未扣 / 子自限
（父 990 富余子尽拒）/ 超补双封顶 / 部分补累积 / 畸形九型（父 0、
null/空白 id、重复、负容量、ghost、负 amount、负补、null map、
ghost 补）fail-fast。
