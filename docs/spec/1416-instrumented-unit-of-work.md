# 1416 — 事务计量装饰器

> 来源：L 会话第 17 轮 = effort #1416（票 T2133 / T2134 / impl 1069）。借鉴：PostgreSQL pg_stat_database（xact_commit/xact_failed 比例是数据库健康第一读数）+ Seata 事务度量。

## Problem Statement

`UnitOfWork` 是存储侧事务 SPI（内存/JDBC/Redis 多实现），但「多少事务、多少失败、败在哪个异常类」零读面：存储抖动/死锁/约束冲突只能从上游业务报错反推。事务成功率的劣化（连接池耗尽、锁等待超时）无先行信号。

## 目标

- `InstrumentedUnitOfWork implements UnitOfWork`（core/transaction，opt-in 装饰器）：
  - 包住宿主既有实现（委托逐方法透传，含 deleteSession——不替换实现）；
  - 三总量：`begun` / `completed` / `failed` + `inFlight` 瞬时；守恒式 `begun = completed + failed + inFlight`；
  - 失败异常类 Top 榜（简单类名，次数降序平名典序；**有界 8** 超出并 OTHERS——基数纪律）；
  - 异常原样上抛（不吞不包装）；双重载（全局/per-session）都计量；
  - `stats()` 嵌套 `record Snapshot` + `resetForTest()`（实例级，不动 delegate）。

## 兼容性

纯 opt-in 装饰器：未包装零开销；委托语义/异常契约逐位不变（RuntimeException 透传上抛）。

## Out of Scope

- 事务时延分位（store 延迟环 spec 810 同族可另叠）。
- 按会话分桶事务数（基数红线）。
- 补偿事务（CompensatingBatch）域的计量（另轴）。
