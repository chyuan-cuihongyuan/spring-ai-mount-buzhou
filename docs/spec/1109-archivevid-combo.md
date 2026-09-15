# 1109 — 归档×evidence 回查联动组合测试轮

> 来源：J 会话第 109 轮 = effort #1109（[T1677](../../.wayfinder/tickets/T1677-archivevid-shape.md) / [T1678](../../.wayfinder/tickets/T1678-archivevid-verify.md) / impl 861）。纯测试轮第十六弹。core/cleanup 与 memory/tool 生命周期联动收口。

## Problem Statement

R75 SessionArchiver（归档级联删除活数据）与 R73 EvidenceLookupTool（回查）联动——归档后回查行为的组合验证缺失：级联删除后 evidence 回查的结局（命中/miss）与双读面守恒。

## 目标

新增组合测试：归档会话后 evidence 回查——双读面各自守恒保持 + 交互行为按 store 语义钉住。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- 归档数据回放面（restore 另轴）。
