---
Type: task
Status: closed
---
## Question

`SecretType`（7 型有界枚举）+ `SecretScanner`（scan/redact、`[SECRET:TYPE]`
占位符、幂等、无命中零改写）。

## Resolution

done（2026-09-08）：impl-373；检测器用例（7 型正/负样本 + 幂等 + 引用等）绿。
