# 731 — PII 假名化模式装配

> 来源：G 会话第 32 轮 = effort #731（D 会话装配轮模式）/ [T1013](../../.wayfinder/tickets/T1013-pii-mode-assembly-shape.md) / [T1014](../../.wayfinder/tickets/T1014-pii-mode-assembly-verify.md) / impl 534。

## 背景

PiiDetector.pseudonymize（spec 713）只有方法面——GuardModule 装配链（MASK 占位符硬编码）不可达。

## 目标

- `PiiDetector(boolean formatPreserving)` 模式构造：true 时 `redact` 分派 pseudonymize（三缝共用同一 detector 实例时模式自然一致）；false = 既有 MASK。
- `PiiRedactionHook` / `PiiInputRedactionHook` 增 3 参构造（types, customRules, formatPreserving）。
- `GuardModule.Builder.piiPreserveFormat()` + 双缝装配直通（三元树顺带收敛为直构造——等价性由 null 兼容构造保证）。

## 测试

piiPreserveFormat 声明 → 输入缝假名化（形状保持非占位符）；缺省 MASK 语义零回归；装配摘要可见。

## 兼容性

缺省逐字节不变；null 兼容构造等价性由既有用例守护。
