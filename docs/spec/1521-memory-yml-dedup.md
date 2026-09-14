# 1521 — MemoryModule yml 解析样板统一（六-5 部分）

> 来源：M 会话第 24 轮 = effort #1521（impl 1124）。design-incompleteness 六-5 部分闭环。

## 背景

MemoryModule 的 yml 子树解析存在 13 处「ymlConfig.get("memory") → instanceof Map → get(key) → 类型判定」嵌套样板。

## 目标

memoryLeaf/memorySub 两 helper 统一 9 处简单提取；3 处复杂消费体（反射加载/泛型 Map 遍历）保留。

## 兼容性

等值重构零行为变化。
