# 1405 — Dashboard 查询页守卫与游标解析修复

> 来源：L 会话第 6 轮 = effort #1405（票 T2111 / T2112 / impl 1058）。借鉴：Grafana query limit（查询侧硬上限守卫数据源——面板查询失控不拖垮存储）。

## Problem Statement

**实证缺陷**（本轮勘察发现）：`DashboardQueryService` 两条翻页路径守卫不对称——
1. `listSessions` 完全不钳制 `size`：`size=10_000_000` 即对 ObservabilityStore 发起无界读取（filtered 路径有 `Math.max(1, Math.min(size, 200))`，本路径零守卫）；`size<=0` 时 `hasMore` 判定恒真走 `subList(0, size)` 抛异常；
2. 两路径游标解析裸抛 `NumberFormatException`（`Integer.parseInt` 无守卫），HTTP 侧 500 带不可读堆栈。

## 目标

- `MAX_PAGE_SIZE = 200` 公共常量（与 filtered 路径既有 200 对齐）；
- `normalizePageSize(size)` 共享钳制：<1 归 1、>200 钳制——`listSessions` 接入（filtered 改用同源）；
- `parseCursor(cursor)` 共享解析：空白归 0、格式非法抛可读 `IllegalArgumentException`（「游标格式非法（须为十进制偏移整数）：xxx」）——两路径同源；
- 翻页语义（size+1 探测/nextCursor 推进）逐位不变。

## 兼容性

行为变化即守卫本身：`size>200` 的请求从「无界读取」变为「钳制 200」（此前为资源风险非契约）；`size<=0` 从异常变为归一 1；非法游标异常类型 NFE→IAE（同为 unchecked，HTTP 侧状态码不变、报文可读化）。既有小页查询零变化。

## Out of Scope

- HTTP 层 400 语义映射（IAE→400 需动 DashboardHttpServer 错误映射，另轮）。
- rollups 桶数上界（MAX_ROLLUP_BUCKETS=1000 已有守卫）。
- 分页深翻页（offset 游标大偏移）的存储侧优化（store 契约域）。
