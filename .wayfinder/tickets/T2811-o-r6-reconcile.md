---
id: T2811
title: O 系 R6 对账轮的裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question

R1–R5 五轮后全仓三门+并行吸收+快照补登怎么收口？（spec 1805 / effort #1805 / R6）

## Resolution

**三件事收口**：1) 全仓 clean verify（隔离 worktree 复现
定位 HEAD 断链后回主树修复）；2) 快照补登——R2–R5 四公共类型 regenerate
+api-surface.md O 系小节入档；3) 并行吸收——J 系 skills 断链两连修复
（11bc4d3a 滑手→005cc8f0×a8b7885d 并发对撞→7fdb1611 和解裁决 succeeded 侧）。
教训入档：kill 在途 verify 留部分编译态（test-classes 内部类缺失假红），
对账轮 verify 必须 clean 起步。
