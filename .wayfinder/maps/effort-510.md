# Wayfinder Map — Buzhou 会话导出加密（effort #510，E 会话第 10 轮）

> E 会话第 10 轮（333 通道扩散轮）。勘察：28 会话导出/导入产物是**明文
> JSON**（toJson/fromJson 可移植边界）——导出文件落盘/传输即携带全部
> 会话内容（消息/摘要/state），敏感会话的导出面空白。333 消息静态
> 信封加密（EnvelopeCipher AES-GCM+AAD+双钥）已在 crypto 包。

## Destination

`session.EncryptedSessionExport`（原语先行，宿主组合 EnvelopeCipher）：
`seal(SessionExport)` → `"buzhou:session-export:v1:" + cipher.encrypt(
toJson(), AAD)`；`open(sealed)` → 校验封缄标记（非封缄/坏标记
DATA_CORRUPTION fail-fast 带修法）→ 解密（AAD=用途域常量
"buzhou.session-export"——消息存储信封的 AAD 是消息标识，跨域剪贴
解不开）→ fromJson（坏 JSON 同样 DATA_CORRUPTION）。`isSealed` 静态
判定。诚实边界：单密钥环进程内口径（333 同）；spill 证据引用不内嵌
（28 同注记——密文化不改变引用语义）。

## Notes

- 号段：spec 510 / T771–T772 / impl-413。
- 借鉴源：age/OCI 加密 artifact（密文容器+标记头）；通道复用 333
  Vault transit/KMS envelope。
- 归档冷存（97/336 摘要加密已有）与本面互补：本面护**导出文件**。

## Out of scope

- autoconfig bean（宿主组合；333 cipher bean 化留后续）；ZIP bundle
  内单会话加密（317 打包面扩散）；口令派生密钥（KMS/master key 口径）。

## Tickets

- [x] [T771 seal/open 封缄原语](../tickets/T771-encrypted-session-export.md)
- [x] [T772 AAD 用途域与 fail-fast](../tickets/T772-export-seal-semantics.md)
