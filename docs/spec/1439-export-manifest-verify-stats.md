# 1439 — 导出清单校验统计读面

> 来源：L 会话第 46 轮 = effort #1439（**补位轮**——1439 号为 R39 重编号后空缺位，按 H 会话 R39 补位先例回填真实机制；票复用空缺号 T2179/T2180 的后继位 T2185/T2186 见台账）→ 实际票 **T2185 / T2186**（R42 后下一空位）/ impl 1098。借鉴：TUF 仓库校验遥测（完整性校验的执行频次与失败分布本身是安全遥测——只建链不校验等于没建）。

## Problem Statement

`ExportManifest`（spec 193 防篡改清单：sha256 摘要链 + verify 三列校验）的 verify 是纯函数——**校验执行了多少次、失败分布如何**无读面：搬运链路上「从不校验」与「校验全红」两种病灶静默。

## 目标

- `ExportManifestVerifyStats`（core/session，进程级静态读面，ToolArgsValidator 先例）：
  - 受踪包装三方法：`verifyCanonical/verifySubset/verify`（委托原实现 + 入账，`entryKind` 记 canonical/subset/full）；
  - 漏斗：`verifies = ok + failed`（conserved 派生）；明细桶 `mismatchedTotal/missingTotal/unexpectedTotal`（非互斥可并存）；
  - `stats()` → `record Snapshot(..., lastEntryKind)` + `resetForTest()`。
- `ExportManifest` 本体零改动（包装类承载统计——比侵入纯函数更干净）。

## 兼容性

纯增量读面：三入口校验语义逐位不变（包装透传）。

## Out of Scope

- 校验耗时（时延归延迟环族）。
- 定期校验调度（宿主例行化）。
- manifest 条目级历史（Snapshot 只累计）。
