# 628 — MCP 每连接并发上限 yml 装配

> 来源：F 会话第 29 轮 = effort #600（spec 610 原语的装配扩散）/ [T906](../../.wayfinder/tickets/T906-mcp-concurrency-assembly-shape.md) / [T907](../../.wayfinder/tickets/T907-mcp-concurrency-assembly-verify.md) / impl 481。

## 背景

每连接并发闸（610）只有编程 setter；stdio 单线程 server 防护需要 yml 声明即生效。

## 目标

`buzhou.mcp.per-connection-concurrency-limit` → 属性 → Builder → 注册表。

## 非目标

- 不做 per-server 差异化（全局一档先行）。

## 设计

缺省 null 零变化；<=0 fail-fast；canonical @ConstructorBinding（R39 坑）。

## 测试

3 用例：绑定/缺省/校验。

## 兼容性

三参构造保留；缺省零变化。
