# Spec 177 — 事件载荷出站脱敏（effort #207）

> wayfinder map：`.wayfinder/maps/effort-207.md`（T547–T548）。PII 防线收口：输入侧
> （106）/ 输出侧（86）/ 自定义（118/129）之后，<b>webhook 出站面</b>是最后
> 一个未盖住的 PII 出口。

## Problem Statement

会话事件（`user.turn.completed` 等）的 payload 常携带输入原文——用户误贴的
身份证/手机号已经过输入侧 hook 脱敏进模型，但事件面若在脱敏<b>前</b>采集、
或宿主直发自定义事件，webhook 出站把 PII 送出系统边界。防线不能只防「进」
不防「出」。

## Solution

`PiiEventRedactor`（guard/pii，`SessionEventListener` 装饰器）：

- **脱敏**：onEvent → payload 中每个 String 值过 `PiiDetector`（内置五型，
  类型集可配）+ `CustomPiiRules`（叠加，与 spec 118/129 同实例可复用）；
  构造新 `SessionEvent`（type/occurredAt 不变）下发被装饰 listener。
- **非 String 值原样**（数字/布尔天然安全）；无命中的 String 值保持同引用
  （零改写零分配——与 hook 侧同纪律）。
- **组合**：webhook fanout 的 sink 包一层即全站出站脱敏；任何
  SessionEventListener（仪表盘/外部总线）同款包装。

## User Stories

1. 作为合规，webhook 接收方拿到的 payload 不含 PII——系统边界两侧口径一致。
2. 作为宿主，已有 CustomPiiRules 实例直接复用——领域格式一处声明处处生效。
3. 作为运维，遥测类全数值事件零脱敏成本（无命中零改写）。

## Implementation Decisions

- 深度一层（payload Map 值为 String 才处理）——嵌套递归留档（诚实边界）。
- 装饰器不吞事件（脱敏失败即原文下发——出站可用性优先，护栏 fail-open）。

## Testing Decisions

- payload 电话/邮箱脱成占位符；自定义规则叠加；非 String 值原样同引用；
  无命中 payload 等价；被装饰 listener 收到的是脱敏版。

## Out of Scope

- 嵌套结构递归；观测存储入站脱敏；per-type 跳过表。

## Further Notes

- PII 防线四面齐：输入（106）/ 输出（86）/ 自定义声明（118+129）/ 出站（本轮）。
