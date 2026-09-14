# 1418 — Redis 慢操作榜

> 来源：L 会话第 19 轮 = effort #1418（票 T2137 / T2138 / impl 1071）。**换题记录**：原题 R33 离群驱逐与 mcp breaker 域（811/814）临界——换入 S4 备选题（Redis SLOWLOG 的客户端侧扩散）。借鉴：Redis SLOWLOG（严格大于阈值才入、有界 FIFO、读面即现场）；客户端视角与服务端慢日志互补。

## Problem Statement

`redis-cli slowlog` 只看**服务端**执行的命令耗时；客户端视角的往返延迟（网络抖动/DNS/序列化/池等待）在服务端慢日志里完全不可见。RedisMessageStore 四主操作（append/load/findById/deleteSession）的慢往返无榜——「这个 Redis 为什么慢」服务端无恙时无据可查。与 J 会话 ToolSlowLog（core/exec 工具侧）同型扩散到 store-redis。

## 目标

- `RedisSlowOpLog`（store-redis，进程级静态面，private 构造）：
  - `record(op, durationMillis)`：**严格大于**阈值才入榜（不达阈值仅一次 volatile 比较——热路径零成本）；异常路径由调用点 finally 语义入账（慢与败正交）；
  - 有界 FIFO `CAPACITY=32`（挤旧）+ `entries()` 新→旧现场（防御拷贝）+ `totalSlowOps()` 累计水位（挤出也累计）；
  - `configureThresholdMillis`（运行时可调，返回旧值；默认 100ms）+ `resetForTest()`（榜/累计/阈值全复位）。
- 埋点：RedisMessageStore append/load/findById 三操作 finally 计时（deleteSession 级联删多键长事务另轮——口径显式）。

## 兼容性

纯增量读面：存储操作返回语义/异常路径逐位不变；未达阈值零入榜动作（volatile 比较成本）。

## Out of Scope

- deleteSession 计时（级联多键删的单次耗时语义混叠——另轴拆分）。
- 按键名/会话维度分桶（敏感面+基数红线）。
- 服务端 SLOWLOG 的代理读取（Lettuce 无此缝合——客户端视角即本轴定位）。
