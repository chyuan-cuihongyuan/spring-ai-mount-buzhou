# 727 — 秘密熵过滤 yml/Builder 装配

> 来源：G 会话第 28 轮 = effort #727（D 会话装配轮模式）/ [T1005](../../.wayfinder/tickets/T1005-secret-entropy-assembly-shape.md) / [T1006](../../.wayfinder/tickets/T1006-secret-entropy-assembly-verify.md) / impl 530。

## 背景

SecretScanner 熵阈值（spec 714）只有直接构造面——GuardModule 装配链不可达。

## 目标

- `GuardModule.Builder.secretMinEntropy(Double)`（null = 关默认）。
- secretScanning 启用时统一构造 `SecretScanner(types, minEntropy)` 直通（types null = 全类型——与旧双分支等价）。
- `SecretScanHook(SecretScanner)` 自带扫描器构造；no-arg 改为 `new SecretScanner()`（语义等价——全类型无熵闸）。
- `GuardModule.hooksView()` 包内观测面（装配测试断言缝）。

## 测试

Builder 声明熵阈值 → beforeTurn 真缝示例键被滤；缺省照常 redact；装配摘要含 SecretScanHook；全模块回归。

## 兼容性

缺省逐字节不变（旧两分支与新统一构造等价）。
