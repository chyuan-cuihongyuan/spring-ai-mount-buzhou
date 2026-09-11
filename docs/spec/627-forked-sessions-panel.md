# 627 — fork 谱系进会话面板

> 来源：F 会话第 28 轮 = effort #600（spec 602 谱系 + spec 346 面板的接线）/ [T904](../../.wayfinder/tickets/T904-forked-panel-shape.md) / [T905](../../.wayfinder/tickets/T905-forked-panel-verify.md) / impl 480。

## 背景

fork 谱系（602）落 state 后无运维读面——「多少活跃会话是分支」是重试/探索流量的直接信号（重试风暴 = 分支占比飙升）。

## 目标

sessions 面板增 `forkedActive { available, count }` 段。

## 非目标

- 不做谱系链展示（单级计数先行）。

## 设计

索引 ACTIVE 分页（≤50k 封顶）逐会话查 `buzhou.fork.source`；state 读面缺席诚实缺席；五参构造（四参兼容）。

## 测试

2 用例：真实 fork 双向钉住计数 / 无 state 面 unavailable。

## 兼容性

面板加段纯增量；构造兼容保留。
