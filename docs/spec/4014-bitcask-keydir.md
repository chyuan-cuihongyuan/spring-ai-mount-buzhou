# Spec 4014 — Bitcask 键目录合并（effort #4014，R15）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6029–T6030，impl 2115）。
> 借鉴：Riak bitcask 追加日志存储。

## Problem Statement

追加日志存储（顺序写零放大）的旧版本与删除留下死字节——**何时
值得重写回收**（写放大换空间的调度杆）缺账面裁决件。

## Solution

`BitcaskKeydir`（core/cleanup，纯账面模型——真 IO 归存储层）：

- append 追加覆盖（旧版入死账、返回偏移）；delete 键出目录
 （字节入死账）；
- totalBytes/deadBytes/deadRatio 三账面（空日志 NaN 诚实）；
- shouldMerge(threshold) 死比门；mergePlan 活键/活字节成本账；
  applyMerge 压实（死账清零、总账对齐活账、键目录不动）。

## User Stories

1. 作为存储作者，写只追加读走目录——覆盖与删除的死区可回收。
2. 作为容量作者，死比超阈才重写（写放大换空间有调度杆）。

## Testing Decisions

- 三写覆盖死账 100/350 与最新偏移直读；门 0.2 真 0.5 假 + 计划
  2 键 250 字节 + 执行后压实对齐且键目录不动；删除并入死账
  150 与活账 200；空账 NaN 诚实 + 门假；畸形五型 fail-fast。

## Out of Scope

- 不做 hint 文件/启动扫描重建；不做多文件轮转（单日志口径）；
  不做 CRC 校验（Crc32C 已覆盖）。

## Further Notes

- 与 SizeTieredMergePicker（尺寸成组）互补；与 KeyCompaction
  同族不同层。
- 里程碑：15/50。
