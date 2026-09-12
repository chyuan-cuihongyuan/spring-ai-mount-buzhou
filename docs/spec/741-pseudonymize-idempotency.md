# 741 — 假名化×幂等占位符互操作补验

> 来源：G 会话第 43 轮 = effort #741（spec 713/731 补验）/ [T1035](../../.wayfinder/tickets/T1035-pseudonymize-idempotency-shape.md) / [T1036](../../.wayfinder/tickets/T1036-pseudonymize-idempotency-verify.md) / impl 545。

## 背景

伪随机替身不是 `[PII:` 占位符形态——readback 纵深（含占位符跳过重处理）在假名化模式下的行为需补验。

## 目标（测试域补验轮）

- 假名化输出再次经过 pseudonymize：无 PII 命中（替身不再是合法手机号/身份证形态）→ 原样返回（幂等）；
- 假名化输出经过 MASK redact：不产生双重脱敏（替身不匹配内置校验型规则时原样）；
- 混合模式语义边界入档。

## 兼容性

纯测试域增量。
