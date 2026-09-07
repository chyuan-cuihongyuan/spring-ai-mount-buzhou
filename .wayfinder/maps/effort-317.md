# Wayfinder Map — Buzhou 导出族 gzip 合流打包（effort #317，C 会话第 18 轮）

> C 会话第 18 轮。导出族 JSONL 各自为战（PII 命中 166 / 成本台账 188 / 影子
> 对照 309 / 错误签名 104 系……）——合规对账要逐个跑逐个收，窗口换班（export
> → reset 循环）没有一揽子快照（fog 152「导出族 gzip 全员化 + 合流打包」项）。

## Destination

`ExportBundle`（core.observation? 归 core.eval 旁？——勘察后定）：把多个命名
JSONL 导出源（`CheckedConsumer<Writer>` 形态）打进一个 ZIP（逐条目 gzip/
DEFLATE 内建）——一文件一窗口；manifest 条目（名 + 行数 + sha256——OCI
manifest 思想，193 对账先例同源）。空源零条目诚实。

## Notes

- 号段：spec 317 / T625–T626 / impl-340。
- 借鉴：tarball/OCI artifact（多载荷一清单）。

## Decisions so far

- java.util.zip.ZipOutputStream（DEFLATE 默认级——gzip 语义内建）。
- manifest.json 首条目：{name, lines, sha256} 数组——离线对账免解压全量。

## Out of scope

- 上传/Sink 投递（归多 sink 族）；增量窗口管理（reset 纪律归宿主）。

## Tickets

- [x] [T625 ExportBundle（zip + manifest 条目 + sha256）](../tickets/T625-export-bundle.md)（impl-340）
- [x] [T626 打包回归（多源/空源/manifest 对账）](../tickets/T626-bundle-close.md)（impl-340）
