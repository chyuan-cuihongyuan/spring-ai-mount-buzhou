# 1119 — compact_now×归档 生命周期组合测试轮

> 来源：J 会话第 119 轮 = effort #1119（[T1697](../../.wayfinder/tickets/T1697-archivevid2-shape.md) / [T1700](../../.wayfinder/tickets/T1700-archivevid2-verify.md) / impl 867）。纯测试轮第十九弹。memory 与 core/cleanup 生命周期联动（memory 依赖 core 方向可引）。

## Problem Statement

SessionArchiver（归档级联删除）与 CompactNowTool（手动压缩）生命周期联动——归档后 compact_now 调用行为（无消息→skipped 桶）双读面守恒无验证。

## 目标

新增组合测试（buzhou-memory）：归档成功后 compact_now 调用——compact 走 skipped 桶（消息已级联删除）+ SessionArchiver.ArchiveStats 守恒保持。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- 归档回放 restore 面（另轴）。
