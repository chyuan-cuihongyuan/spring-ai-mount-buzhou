# 714 — 秘密扫描熵阈值过滤

> 来源：G 会话第 15 轮 = effort #714（借鉴 trufflesecurity/trufflehog entropy detection）/ [T979](../../.wayfinder/tickets/T979-secret-entropy-shape.md) / [T980](../../.wayfinder/tickets/T980-secret-entropy-verify.md) / impl 517。

## 背景

SecretScanner（spec 400）纯签名正则——文档示例键（AWS 官方示例 `AKIAIOSFODNN7EXAMPLE`）、占位串（`sk-aaaa…`）同样命中，redact 把正常内容误杀成占位符。gitleaks/truffleHog 的第二把尺子：真密钥是高熵随机串——香农熵过滤掉「长得像但不像随机」的命中。

## 目标

- `SecretScanner(Set<SecretType>, Double minEntropyBitsPerChar)` 可选构造：命中文本 Shannon 熵（字符频率，bits/char）< 阈值 → 丢弃该命中；null/≤0 = 关（默认，既有构造零变化）。
- `DEFAULT_MIN_ENTROPY = 4.0`：真随机键（base62/base64 域）4.5+；AWS 文档示例 ≈3.7 恰被滤——示例/占位与真键的常用分界。
- **PRIVATE_KEY_BLOCK 豁免**：签名是 BEGIN 行字面（结构性事实非熵信号），熵过滤会误滤低熵行。

## 非目标

不做 per-type 差异化阈值（统一 4.0 起步）；不做置信度分级输出（滤/不滤二值）。

## 测试

高熵键过/示例键滤/低熵串滤（熵开）；同串熵关命中（对照）；PRIVATE_KEY_BLOCK 豁免；默认构造零回归。

## 兼容性

opt-in 纯增量；默认构造逐字节不变。
