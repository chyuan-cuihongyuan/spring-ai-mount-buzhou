# Wayfinder Map — Buzhou 秘密命中统计与导出（effort #418，D 会话第 19 轮）

> D 会话第 19 轮（#400 扩散轮——spec 400 out-of-scope 注记的「命中 JSONL
> 导出」收口）。勘察：PII 有 PiiHitStats（全局持有+分侧）+PiiHitStatsJsonl；
> #400 的 SecretScanHook 只打指标计数——**命中明细无统计面无导出面**，
> 泄漏面趋势（哪个类型/哪条缝常出）不可分析。

## Destination

`guard.secret.SecretHitStats`（PiiHitStats 同构镜像）：全局持有
install/global；三侧枚举 INPUT（beforeTurn）/OUTBOUND（beforeTool 出站
参数）/OUTPUT（afterTool 结果）；per-type 计数 + snapshot()（排序报表行）。
SecretScanHook 三缝 record 入账（构造零改动）。`SecretHitStatsJsonl`：
snapshot 行落盘（{at,name,side,count}——追加快照口径与 Pii 同族；宿主
用 DelayedJobQueue/定时任务周期落盘——库级不装配）。

## Notes

- 号段：spec 418 / T727–T728 / impl-391。
- 借鉴源：PiiHitStats/PiiHitStatsJsonl（本库 313/86 先例）；gitleaks
  report 口径（命中面要可分析）。
- 纪律：type 有界（SecretType 7 值枚举）；snapshot 不清零（追加快照——
  趋势对比用两次快照差）。

## Out of scope

- 周期自动落盘装配（宿主调度——DelayedJobQueue 组合即得）；热路径开销
  预算；面板端点（时间线族扩散候选）。

## Tickets

- [x] [T727 SecretHitStats](../tickets/T727-secret-hit-stats.md)
- [x] [T728 hook 记账 + JSONL 导出](../tickets/T728-secret-stats-jsonl.md)
