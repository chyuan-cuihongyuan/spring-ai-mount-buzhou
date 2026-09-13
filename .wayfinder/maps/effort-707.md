# effort #707 — spill 双文件配对完整性巡检（换序轮）

- 会话：G 会话 700 系第 8 轮 ｜ spec [707](../../../docs/spec/707-spill-pair-audit.md) ｜ 票 [T1014](../tickets/T1014-spill-pair-audit.md)/[T1015](../tickets/T1015-spill-pair-audit-verify.md) ｜ impl607
- **换序注记**：原 R8「会话操作 reflog」勘察确认会话事件已经由 obs:events 持久化面覆盖（per-session ZSET 索引+正文）——reflog 增量价值不足，按池规则弃题；原 R9「spill 压实读数」深化为本题提前。
- 借鉴：Git fsck（≈52K star）object 完整性——悬空对象在事故前可见；S3 multipart 残留清理同思想

## 勘察（排重）

- DiskSpillStore=每条目双文件（.spill+.meta，writeAtomically×2）——**两写之间崩溃**留下 data-without-meta / meta-without-data；sweepOrphans（impl-38）只按**会话存活**扫——属主会话活着时文件对残缺永远无人发现，配额被静默吞噬。
- ReadIntegrity（539）是读时内容校验（sha256）；EvidenceRefLedger 是 fork 引用计数——**结构层配对完整性**无面。
- grep PairAudit/orphan：无 spill 配对面。

## 决定

`SpillPairAudit`（spill 纯函数）：audit(rootDir)→Report——dataFiles/metaFiles 总数+dataBytes+findings（DATA_WITHOUT_META 带 bytes / META_WITHOUT_DATA）；uri 用相对根的路径。只读不删（清理动作归 housekeeper 接线轮）；目录缺席=空 Report（未用 spill 诚实零）。

## 测试

健康对不报/孤 data 带字节数/孤 meta/目录缺席空报告/null fail-fast。

## 诚实边界

只读不修复；不校验内容哈希（读时 539 已管）；不模拟 fork 引用（账本层另有其职）。
