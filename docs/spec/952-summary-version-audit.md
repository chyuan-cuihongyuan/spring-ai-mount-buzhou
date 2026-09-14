# 952 — 摘要版本链缺口审计

> 来源：I 会话第 51 轮 = effort #951（impl 694 续）。借鉴：Kafka log gap / consumer offset 对账——版本链非连续即「有摘要被外部删除或写入失败」。

## 背景

`SummaryStore.save` 返回每会话单调递增 version（AtomicLong 分配）。外部清理/存储故障会造成版本缺口——「链上缺了哪几版」无读面（缺口 = 审计连续性信号，与审计链 hash 链断裂同级）。

## 目标

- spi 包新公共纯函数类 `SummaryVersionAudit`：
  - `gaps(List<StructuredSummary> summaries)`：按 version 升序扫描，返回 `List<Long> missingVersions`（相邻 version 间跳过的号）；
  - 校验：summaries null fail-fast；version ≤ 0 fail-fast；同 version 重复 fail-fast（存储唯一性口径）；
- 纯函数零 IO；不改动 SummaryStore 接口（default 方法会破坏既有实现二进制兼容——诚实划界）。

## 兼容性

纯增量：新公共类型，零既有行为变化。
