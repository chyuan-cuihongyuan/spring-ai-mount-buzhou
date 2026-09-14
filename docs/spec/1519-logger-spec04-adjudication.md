# 1519 — System.Logger 双门面追认 + spec 04 mcp 属性回写

> 来源：M 会话第 22 轮 = effort #1519（impl 1122）。design-incompleteness 五-3/四-7 闭环。

## 裁定

- 五-3：SLF4J 与 System.Logger 双门面追认（69 文件既成风格不迁移）；占位符风格（禁拼接/禁丢栈）硬约束不变；同文件不混用；
- 四-7：spec 04 补 mcp 装配属性增量（dangerous-tool-patterns 缺省七动词 / shutdown-budget 35s / per-connection-concurrency-limit）+ config-reference 指针。

## 兼容性

纯文档零行为变化。
