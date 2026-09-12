# 713 — PII 格式保持假名化

> 来源：G 会话第 14 轮 = effort #713（借鉴 microsoft/presidio 格式保持 surrogate 思想）/ [T977](../../.wayfinder/tickets/T977-pii-pseudonymize-shape.md) / [T978](../../.wayfinder/tickets/T978-pii-pseudonymize-verify.md) / impl 516。
> 选题注记：原列主题「双 judge Kappa」缺口核查已被 spec 541 JudgeAgreement 覆盖——ruled-out 顺延。

## 背景

现有 PII 两面各有盲区：`redact` 全占位符（`[PII:TYPE]`——长度/形态尽失，下游格式分析不可用）；`PiiVault`（spec 507）可逆 token（`[PII-VAULT:hash]`——长度不保，且引入原文托管义务）。缺一个「保形状、不可逆、零托管」的中间档：位数统计、分隔形态、大小写分析等下游处理仍可运行。

## 目标

- `PiiDetector.pseudonymize(text, enabled)`：逐命中生成同长度同字符形态替身——数字→伪随机数字、字母→同大小写伪随机字母、分隔符/空白/符号原样。
- 确定性：替身随机流按 `(SURROGATE_SEED 常量, type, 命中文本)` 哈希播种——同 (type,文本) 恒同替身（进程内外一致）；无共享可变态，线程安全。
- **诚实边界**：保形状不保校验位——身份证 mod-11 / 银行卡 Luhn 在替身上不再验真（真 FPE/FF1 需密码学实现，非目标；校验失败是遮蔽生效的预期信号）；不可逆、原文零留痕（可逆需求归 PiiVault 互补面）。

## 非目标

不做 FF1/FPE 密码学实现；不做 vault 联动（pseudonymize 与 vaultize 互斥选型由调用方）；不改 redact/vault 既有语义。

## 测试

三类命中形状保持；确定性（两次调用逐字符同）；原文不留痕；非 PII 原样；既有用例零回归。

## 兼容性

additive 方法；既有 redact/scan/vault 语义零变化。
