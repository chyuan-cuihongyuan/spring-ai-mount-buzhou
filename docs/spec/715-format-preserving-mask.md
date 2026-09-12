# 715 — PII 格式保形掩码

> 来源：G 会话第 16 轮 = effort #715（PII 族展示层深化）/ [T1030](../../.wayfinder/tickets/T1030-fp-mask.md) / [T1031](../../.wayfinder/tickets/T1031-fp-mask-verify.md) / impl 615。

## Problem

PII 脱敏现有两形态：`[PII:TYPE]` 占位符（redact——下游解析器再读不出）与 507 vault（可逆代管——要回显授权）。第三形态缺失：**展示层格式保形掩码**——`138****5678` 让人工一眼可辨形态（客服核对、日志平台、表单回显），下游按位数/分段解析的逻辑不被破坏。Presidio 的 format-preserving 思想（结构保形）。

## Solution

- `FormatPreservingMasker`（guard/pii，纯静态原语）：
  - `maskPhone`：CN 手机 11 位保前 3 后 4（`138****5678`）；
  - `maskIdCard`：18 位身份证保前 4 后 2（尾位校验码可见）；
  - `maskEmail`：保首字符与 @ 后域名（`a***@example.com`）；
  - `maskIp`：保前两段（`192.168.*.*`）；
  - `mask(text, keepHead, keepTail)`：通用保长中段打星；
  - 形状校验 `isValidPhone/isValidIdCard/isValidEmail/isValidIp`；**校验失败 → 等长全星**（fail-closed 不抛——掩码管道里坏数据不炸流程）。

## User Stories

1. 客服回显：工单系统用 507 vault 取回后展示 `138****5678`——形态可核对、不泄全量。
2. 日志治理：IP 掩到前两段——运维可聚合定位网段，个人定位不可。

## Implementation Decisions

- 结构化掩码非密码学 FPE（同输入同掩码——强去标识归 507/SpillCipher 族，诚实边界）。
- 形状校验复用 PiiDetector 既有正则口径（PHONE/ID_CARD/EMAIL/IP）——检测与掩码两侧不漂移。
- 身份证尾位 X 保留（校验码语义）。

## Testing Decisions

- 四型标准输入掩码形态逐字符断言；长度不变断言。
- 非法形状（位数错/非数字）→ 等长全星；null → NPE fail-fast。
- 通用掩码边界：keepHead+keepTail ≥ 长度 → 全保（不产生负数中段）。

## Out of Scope

- 密码学 FPE（NIST FF1）。
- 非中国口径证件（宿主通用掩码自拼）。
- 自动挂接 redact 管道（替换 `[PII:TYPE]` 语义是行为变更——宿主自选）。

## Further Notes
