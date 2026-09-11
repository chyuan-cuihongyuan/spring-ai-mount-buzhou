# 610 — MCP 每连接并发上限

> 借鉴：[modelcontextprotocol](https://github.com/modelcontextprotocol/modelcontextprotocol) 实现生态对同连接并发请求的客户端侧限流惯例（stdio 单线程 server 尤其敏感）。
> 来源：F 会话第 11 轮 = effort #600 / [T870](../../.wayfinder/tickets/T870-mcp-conn-concurrency-shape.md) / [T871](../../.wayfinder/tickets/T871-mcp-conn-concurrency-verify.md) / impl 463。

## 背景

buzhou 并行工具 fan-out（虚拟线程）会把多个工具调用同时打到同一 MCP server 连接；stdio 单线程实现对并发调用敏感（排队/错乱/崩溃）。模型并发舱（spec 426）管 per-model 在飞，MCP 连接层无闸。

## 目标

`DefaultMcpClientRegistry.setPerConnectionConcurrencyLimit(Integer)`：每连接信号量闸，包装层 acquire/release。

## 非目标

- 不做协议级并发能力协商（server 声明——雾区）。
- 不跨连接共享额度（per-connection 正交语义）。
- 既有条目不追溯（Entry 创建时装配——诚实边界）。

## 设计

- null/<=0 = 不设（默认零行为变化）；<=0 显式拒绝。
- 执行语义 = 阻塞可中断获取：虚拟线程便宜、core 工具超时兜底总时长；中断走失败转文本（与摘除拒绝同词汇，不抛不炸工具循环）。
- 许可与引用计数正交：先引用（DRAINING 拒绝语义不变）再许可。

## 测试

4 用例：limit=1 第二调用阻塞至第一释放、跨连接互不影响、默认不设真并发、上限校验。

## 兼容性

默认不设 = 完全直通；新增 setter 与包装层内层 try 纯增量。
