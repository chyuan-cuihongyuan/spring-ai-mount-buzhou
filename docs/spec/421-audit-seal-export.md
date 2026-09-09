# Spec 421 — 审计封印导出（effort #421）

> wayfinder map：`.wayfinder/maps/effort-421.md`（T733–T734）。D 会话第 22 轮。

## Problem Statement

sealMerkle() 封印只在内存（有界 32 环）；CT log 的 STH 公示节奏
（定期导出根→外存→事后比对）无工具面——根没离开进程就谈不上对外
承诺。

## Solution

`guard.audit.AuditSealJsonl.appendSeals(AuditChain, Path)`（418 追加快照
同族）：

- 逐行追加当前全部封印 {sealedAt(epochMs), recordCount, rootHex,
  **verified**}；verified = 以当前全记录建树，树根 == 该印根（旧印在
  追加后恒 false——「印后又有记录」属正常，语义注记：只有**最新一印**
  期望 true，否则链被动过）。
- 追加式（印有界 32 → 每轮 ≤32 行）；父目录自动创建；写失败上抛。
- 宿主定时调用（DelayedJobQueue 组合）= CT 式公示节奏。

## User Stories

1. 作为审计方，我想定期拿到封印行外存，so 根承诺可离线比对。
2. 作为安全负责人，我想最新印 verified=false 时一眼可见，so 链被动
   篡改的信号不被埋没。

## Testing Decisions

- 一印后导出：1 行 verified=true；追加记录后再印再导出：2 行（旧 false
  新 true）；空链 0 行。历史行冻结在导出时点的验证值（追加式文件——
  第一次导出的行不回写，公示时刻为真即 CT STH 语义）。

## Out of Scope

- 定时装配；印签名；外部公证对接。

## Further Notes

- 新公共类型 `AuditSealJsonl` 随轮 regenerate 快照。
