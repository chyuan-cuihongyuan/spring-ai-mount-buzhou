# 828 — 计时连接池 DataSource 装饰器

> 来源：H 会话第 29 轮 = effort #828 / [T1157](../../.wayfinder/tickets/T1157-timed-datasource.md) / [T1158](../../.wayfinder/tickets/T1158-timed-datasource-verify.md) / impl 581。
> 借鉴：HikariCP 池等待指标（≈20K star）。

## Problem

「查询慢」有两种成因：慢在拿连接（池耗尽等待）vs 慢在执行。810 计的是 store 操作整体——连接获取层无独立计量。

## Solution

`TimedDataSource`（store-jdbc，DataSource 装饰器）：

- **计时**：getConnection / getConnection(user,pass) nanoTime finally 计时进 StoreLatencyRing（异常照抛、耗时照记）。
- **委托**：其余 DataSource 方法（logWriter/loginTimeout/unwrap/isWrapperFor/parentLogger）纯透传。
- **接线**：装配侧包装池 bean（Spring Boot 自动装配零侵入）。

## 兼容性

纯新增装饰器；JdbcStores/自动装配零变更。

## 诚实边界

getConnection 耗时含池等待+建连+驱动（不可细分——内部指标归 Hikari 自身）；ring 复用 810（操作名区分域）。
