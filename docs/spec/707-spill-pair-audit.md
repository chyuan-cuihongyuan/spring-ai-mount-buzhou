# 707 — spill 双文件配对完整性巡检

> 来源：G 会话第 8 轮 = effort #707（spill 结构层完整性；换序注记见 map）/ [T1014](../../.wayfinder/tickets/T1014-spill-pair-audit.md) / [T1015](../../.wayfinder/tickets/T1015-spill-pair-audit-verify.md) / impl 607。

## Problem

DiskSpillStore 每条目两个文件（.spill 数据 + .meta 元数据），store() 用两次 writeAtomically 写入——**两写之间崩溃**（进程被杀/磁盘满在第二次写前）留下残缺对：

- data-without-meta：数据在但元数据（sha256/尺寸/时刻）丢失——读路径无法验证完整性；
- meta-without-data：元数据在但数据丢失。

现有治理全部接不住：sweepOrphans（impl-38）按**会话存活**清扫——属主会话活着时残缺对永远不是「孤儿」；ReadIntegrity（539）是**读时**内容校验——读不到/读坏了才报，残缺文件平日静默占配额。磁盘配额（SpillQuota）被无人能读的文件慢慢吃光。

## Solution

Git fsck 悬空对象思想（≈52K star）——结构完整性独立于读时内容校验：

- `SpillPairAudit`（spill，纯函数静态原语）：
  - `audit(Path rootDir)` → `Report(findings, dataFiles, metaFiles, dataBytes)`；
  - 扫描 root 下三层布局（agent/session/文件），.spill 无配对 .meta → `DATA_WITHOUT_META`（带字节量——吞噬量可见）；.meta 无配对 .spill → `META_WITHOUT_DATA`；
  - uri=相对 root 的路径（可读可定位）；健康对不计 findings；
  - root 目录不存在 = 空 Report（未启用 spill 的诚实零，与 sweepOrphans 的 `!isDirectory→0` 同口径）。
- 只读不删：清理是破坏性动作——housekeeper 接线轮再议（538 巡检「findings WARN+计数不自动修复」同纪律）。

## User Stories

1. 巡检：定期 audit——「3 个残缺对、共 45MB 配额被吞」一屏可见，衰变在 IO 异常暴露前发现。
2. 事后：磁盘满排障——audit findings 直接定位到残缺文件（相对路径），不用手翻目录树。

## Implementation Decisions

- 纯函数无 bean（538「先原语后接线」同节奏）；`Files.walk` 一次遍历（目录量级=会话数，遍历成本可忽略）。
- uri 归一用正斜杠（跨平台报告一致）。
- bytes=文件尺寸（File.toPath 懒加载 size）——data 吞噬量的直接口径。

## Testing Decisions

- @TempDir 植文件：健康对（.spill+.meta）不报；孤 .spill 报 DATA_WITHOUT_META 带 bytes；孤 .meta 报 META_WITHOUT_DATA；总数/字节统计精确。
- root 不存在 → 空 Report；null path fail-fast。

## Out of Scope

- 自动清理（housekeeper 接线轮）。
- 内容哈希校验（539 读时已管）。
- fork 引用模拟（EvidenceRefLedger 层职责）。

## Further Notes

三层结构完整性矩阵：sweepOrphans（会话层）→ 本面（文件对层）→ ReadIntegrity（内容层）——spill 完整性三道防线各司其职。
