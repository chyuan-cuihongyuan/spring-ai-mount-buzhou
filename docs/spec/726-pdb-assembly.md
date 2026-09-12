# 726 — 归档 PDB yml 装配

> 来源：G 会话第 27 轮 = effort #726（D 会话装配轮模式）/ [T1003](../../.wayfinder/tickets/T1003-pdb-assembly-shape.md) / [T1004](../../.wayfinder/tickets/T1004-pdb-assembly-verify.md) / impl 529。

## 背景

SessionAvailabilityFloor（spec 704）只有编程构造——`buzhou.cleanup.min-available-sessions` 声明式入口缺失。

## 目标

- `buzhouSessionArchiver` bean 增 Environment + ObjectProvider\<SessionIndexStore\> 参数：属性 >0 且索引在场时构建 floor。
- **capped probe 计数**：liveSessions = `index.list(query(limit=min+1)).size()`——不数全量（归档频率下 O(min) 一页读即答「>min 否」）；min=0 时装配面不建 floor（但语义本身保护最后一个会话）。

## 测试

capped probe 三态（高于/恰在/低于 min）；与 spec 704 语义一致；全模块回归。

## 兼容性

缺省（属性缺省 0）逐字节不变。
