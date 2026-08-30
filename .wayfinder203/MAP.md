# Wayfinder Map — Buzhou 工具结果裁剪装饰器（effort #203，B 会话第 26 轮）

> B 会话第 26 轮。观察：工具返回常是嵌套 JSON 大包，模型只需要一个字段——
> 现在只能整包进上下文（限幅器只截断不提炼）。借鉴 Vector VRL（声明式
> 数据变换）与 jq 的「取你要的」思想。

## Destination

TransformingToolCallback（core/exec 装饰器）：宿主给 UnaryOperator<String>
裁剪函数（提字段/抽段落/清噪），工具结果入上下文前过变换；变换异常/空
→ 原样（fail-open——变换是尽力提炼，绝不丢数据）。定义透传。

## Notes

- 号段：B=奇数 spec（本轮 169）。
- 与 ToolResultLimiter（截断护栏）互补：那是保底防护，这是宿主主动提炼。
- 装饰器家族第四员（retry/memo/transform）——同款透传契约。

## Decisions so far

- fail-open：变换永不吞结果。

## Not yet specified

- 内置常用变换库（jsonField/regexSection 便捷工厂）；yml 声明式。

## Out of scope

- 沿用各轮；流式变换；schema 感知变换。

## Tickets

- [x] [T533 TransformingToolCallback（fail-open 变换装饰器）](tickets/T533-transform.md)（impl-298）
- [x] [T534 裁剪回归（变换生效/异常回退/组合/透传）](tickets/T534-transform-tests.md)（impl-298）
