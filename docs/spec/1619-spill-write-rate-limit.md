# 1619 · spill 写入字节率限速（RocksDB rate limiter 思想）

> 来源：N 会话 R20（effort #1619 / T2389–T2390 / impl 1172）。借鉴对象：RocksDB
> rate limiter——后台写盘按令牌桶节流，防 compaction/flush 高峰打满磁盘带宽。

## Problem Statement

spill 溢出写盘（MB 级正文）在工具执行高峰可能瞬间打满磁盘带宽——同机的
事务日志、页缓存、其他会话的读写都被拖慢。溢出本身是低优先级旁路数据，
不该以满速与关键路径抢 IO。

## Solution

`SpillWriteRateLimiter(bytesPerSecond, burstBytes, maxWaitMillis)`：
- 令牌桶惰性补充（时间差补令牌，无后台线程）；acquire(bytes) 阻节流等待
  （ReentrantLock + Condition——虚拟线程 unmount 不 pin，spec 1606 系纪律）。
- **软限速**：等待超 maxWait 放行 + degradedBypasses 计数——限速器配置跟不上
  实际写压时溢出保护仍生效（限速器故障不放大成 spill 失败）。
- 中断恢复位后放行（溢写保护优先于节流）。
- opt-in：null / bytesPerSecond ≤ 0 = 关（默认零行为）。
- DiskSpillStore.store 写盘前节流（正文 content 字节数）。

## Testing Decisions

- `SpillWriteRateLimiterTest` 五断言：burst 内立即可写（<100ms）+ 超速等待发生
  （≥50ms 宽松下界防 CI 抖动）；超时放行 + degraded 计数；关闭态零开销；
  store 集成（burst 100 < 200 字节 → 节流 → 超时放行写入成功）；配置校验。
- 回归：spill 全量 180 用例。

## Out of Scope

- meta 写的节流（KB 级小文件——收益微小）。
- 全局磁盘 IO 层限速（JVM 层做不了——诚实边界）。

## Further Notes

- 本轮含共用工作区邻居改动的就地修复：DiskSpillStore 错误消息上下文增强中
  未定义的 metaPath 引用改为 sessionDir（并行会话半成品 bug，编译解卡）。
