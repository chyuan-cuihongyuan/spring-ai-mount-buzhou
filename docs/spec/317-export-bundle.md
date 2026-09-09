# Spec 317 — 导出族 gzip 合流打包（effort #317）

> wayfinder map：`.wayfinder/maps/effort-317.md`（T625–T626）。借鉴：tarball/OCI
> artifact（多载荷一清单——fog 152「导出族合流打包」项）。

## Problem Statement

导出族 JSONL 各自独立：合规换班窗口（export → reset 循环）要逐个导出逐个
收集——没有「一文件一窗口」的一揽子快照，多源对账靠人工凑。

## Solution

`ExportBundle`（core cleanup 旁的导出域，`core.export`）：

- `bundle(Path zip, LinkedHashMap<String, ExportSource>)`——命名导出源
  （`long write(Writer)` 形态，与 PiiHitStatsJsonl/ModelCostLedgerJsonl 同构）
  逐条目写入 ZIP（DEFLATE）。
- 首条目 `manifest.json`：[{name, lines, sha256}]——离线对账免解压全量
  （193 防篡改清单同源思想）。
- 空源写零条目（诚实空）；导出源异常逐源隔离（该源记 error 行，其余继续
  ——换班不因单源故障全废）。

## User Stories

1. 作为合规工程师，窗口末一个 zip 拿全所有报表——manifest 即对账清单。
2. 作为运维，单源故障不废整个换班包（error 条目留痕）。

## Implementation Decisions

- ZIP 而非 tar.gz（JDK 内建流式；条目级 DEFLATE）。

## Testing Decisions

- `ExportBundleTest`：多源打包读回逐条目断言 / manifest 行数与 sha256 对账 /
  空源零条目 / 单源故障隔离（error 条目 + 其余源完好）。

## Out of Scope

- 上传投递；窗口增量管理。

## Further Notes

- 导出族：会话导出（28）/ gzip（194/206）/ 防篡改清单（193）/ JSONL 族
  （104/166/188/309）/ **合流打包（本轮——窗口一揽子收口）**。
