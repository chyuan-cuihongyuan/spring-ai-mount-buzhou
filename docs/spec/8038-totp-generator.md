# Spec 8038 — TotpGenerator（effort #8038，V39）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8077–V8078，impl 2340）。
> 借鉴：RFC 6238/4226 Google Authenticator 思想。

## Problem Statement

动态口令的病：依赖真实时钟不可测——**TOTP=HOTP(时间步计数)**，步长注入完全确定。

## Solution

TotpGenerator（core/crypto）：HMAC-SHA1+动态截断+code(secret,counter,digits)+verify(secret,counter,code,window)+RFC 4226/6238 官方向量钉死+null fail-fast。

## Testing Decisions

RFC 4226 HOTP 十向量逐字节+RFC 6238 T=59 → 94287082（8 位 SHA1）+窗口重放语义；fail-fast。

## Out of Scope

- 不做参数化变体（固定经典参数——明示）。

## Further Notes

- 勘误入档：原拟 Wave 7 第五件 ReedSolomon（GF(256) 算术面）
  实现预算超限退雾区——ChineseRemainder 补位（V42 后波次
  内调序——同族加密信任主题不变）。
- 里程碑：V39/50。
