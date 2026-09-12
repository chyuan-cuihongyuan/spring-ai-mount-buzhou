# Wayfinder Map — Buzhou 归档冷存完整性校验（effort #511，E 会话第 12 轮）

> E 会话第 12 轮（97/103/120 归档族扩散轮）。勘察：归档冷存写入后**无
> 完整性证据**——可读 ≠ 未被改（冷层衰变/误写/篡改都会静默通过 restore）。
> S3 checksum 思想：写时校验和随条目落盘、随时可验。

## Destination

SessionArchiver 扩展（internal 类自由改）：`archive()` 写条目同事务写
sha256 hex 校验和（独立命名空间 `__buzhou.archive-checksum__`——不与
`archive.` 前缀同域，countByPrefix/清单扫描语义零变化）；`verify(sid)`
→ VerifyResult（OK/CHECKSUM_MISMATCH/NO_CHECKSUM 存量归档/CORRUPT/
NO_ARCHIVE 五态；MISMATCH detail 可见在案校验和）；`verifyAll()` 清单
序全量校验；restore/purgeExpired/补偿 undo 级联清 checksum。

## Notes

- 号段：spec 511 / T773–T774 / impl-414。
- 借鉴源：S3 additional checksum（写时校验和+读时验证）。
- 诚实边界：NO_CHECKSUM=本特性前存量归档（占位可见不冒充 OK）；校验和
  不防「校验和与内容同被改」（防衰变/误写，不防蓄意攻击——蓄意攻击
  归 333 加密/404 Merkle 面）。

## Out of scope

- 健康面接入（verifyAll 接 ArchiveHealth details 留扩散）；周期自动
  校验（331 选主家务族候选）；加密归档。

## Tickets

- [x] [T773 写时校验和与五态校验](../tickets/T773-archive-checksum.md)
- [x] [T774 级联清理与清单去污染](../tickets/T774-archive-checksum-cascade.md)
