# 1531 — 工具瞬断重试装配链测试（spec 1511 补账）

> 来源：M 会话第 35 轮 = effort #1531（impl 1134）。

## 目标

- IdempotentRetryAssemblyTest（EvalPrune 先例）：enabled 声明即 Holder 生效四断言；缺省零装配；
- 修复装配测试实证的 Duration 转换缺陷：DurationStyle 宽松解析（简写+ISO，非法 fail-fast）。

## 兼容性

装配缺陷修复（Duration 键此前在字符串源下炸装配）；语义面零变化。
