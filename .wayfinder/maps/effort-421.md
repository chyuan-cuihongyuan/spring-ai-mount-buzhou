# Wayfinder Map — Buzhou 审计封印导出（effort #421，D 会话第 22 轮）

> D 会话第 22 轮（#404 扩散轮——spec 404 out-of-scope「印对外发布通道」
> 收口）。勘察：sealMerkle() 产出的封印只在内存（有界 32 环）——根要
> 「对外发布」需人工取数；CT log 的 STH 定期公示节奏（导出→外存→比对）
> 无工具面。

## Destination

`guard.audit.AuditSealJsonl`：`appendSeals(AuditChain, Path)`——把链上全部
> 当前封印逐行追加 JSONL（{sealedAt, recordCount, rootHex}）并即时对当前
> 全记录建树验证根一致性（行带 verified 布尔——链后续追加不破坏旧印的
> 根；当前树根≠最新印根即 verified=false 诚实暴露「印后又有记录」属正常，
> 注记语义）。宿主定时（DelayedJobQueue）调用即得 CT 式公示节奏。

## Notes

- 号段：spec 421 / T733–T734 / impl-394。
- 借鉴源：CT log STH 定期公示 + 418 SecretHitStatsJsonl 追加快照同族。
- 纪律：追加式（每轮全印重写行——印有界 32 行数有界）；失败上抛
  （导出是显式动作该红）。

## Out of scope

- 定时装配（宿主调度组合）；印签名（STH 签名——404 注记同）；外部
  公证服务对接。

## Tickets

- [x] [T733 AuditSealJsonl](../tickets/T733-audit-seal-jsonl.md)
- [x] [T734 验证语义用例](../tickets/T734-seal-export-semantics.md)
