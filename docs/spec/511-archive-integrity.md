# Spec 511 — 归档冷存完整性校验（effort #511）

> wayfinder map：`.wayfinder/maps/effort-511.md`（T773–T774）。E 会话第 12 轮。

## Problem Statement

归档冷存（97）写入后无完整性证据——冷层衰变/误写/篡改都会静默通过
restore（可读 ≠ 未被改）。S3 additional checksum 思想：写时校验和随
对象落盘，随时验证。

## Solution

SessionArchiver 扩展：

- **写时校验和**：archive() 在同一 saga step 内写
  `sha256(json)` hex——独立命名空间 `__buzhou.archive-checksum__`
  （不与 `archive.` 前缀同域——countByPrefix/清单/详情扫描语义零变化）。
- **五态校验**：`verify(sid)` → OK / CHECKSUM_MISMATCH（detail=在案
  校验和可见）/ NO_CHECKSUM（本特性前存量归档——占位可见不冒充 OK）/
  CORRUPT（JSON 不可解码）/ NO_ARCHIVE；`verifyAll()` 字典序全量。
- **级联清理**：restore / purgeExpired / 补偿 undo 均同删校验和键。

## User Stories

1. 作为运维，我想随时验证归档冷存的完整性， so 衰变/误写/篡改在 restore
   前就被发现（而非恢复出坏数据）。
2. 作为合规审计，我想区分「存量无校验和」与「校验和不符」， so 升级
   期不误报、真问题不静默。

## Implementation Decisions

- 校验和不防「校验和与内容同被改」（防衰变/误写不防蓄意——蓄意攻击归
  333 加密/404 Merkle 面，分层诚实）。
- 独立命名空间而非同域子前缀：前缀扫描族（清单/详情/计数）零污染。

## Testing Decisions

- archive→verify OK；直接改写归档值（模拟冷层篡改）→ MISMATCH；
  删校验和键 → NO_CHECKSUM；坏 JSON → CORRUPT；restore 级联清
  checksum；verifyAll 字典序全覆盖；既有 purge/补偿/健康测试零回归。

## Out of Scope

- 健康面接入；周期自动校验；加密归档。

## Further Notes

- 嵌套类型（VerifyResult/VerifyState）不新增顶层公共面（快照零 diff）。
