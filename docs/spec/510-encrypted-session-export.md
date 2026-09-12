# Spec 510 — 会话导出加密（effort #510）

> wayfinder map：`.wayfinder/maps/effort-510.md`（T771–T772）。E 会话第 10 轮。

## Problem Statement

28 会话导出产物是明文 JSON——导出文件落盘/传输即携带全部会话内容。
333 信封加密已护存储面（消息/摘要），导出文件面空白。

## Solution

`session.EncryptedSessionExport`（333 EnvelopeCipher 通道扩散，原语先行）：

- `seal(SessionExport)` → `"buzhou:session-export:v1:" + envelope`——
  密文容器 + 版本标记头（age/OCI 加密 artifact 形态）。
- `open(sealed)`：非封缄/坏标记 → `BuzhouException(DATA_CORRUPTION)`
  带修法文案；AAD = 用途域常量 `buzhou.session-export`——消息存储
  信封（AAD=消息标识）跨域剪贴解不开（333 AAD 绑定语义延续）；坏
  JSON 同样 DATA_CORRUPTION。
- `isSealed(String)` 静态判定（宿主路由明文/密文两形态）。

## User Stories

1. 作为安全宿主，我想导出敏感会话为密文容器， so 导出文件落盘/传输
   不泄露会话内容（密钥不出进程）。
2. 作为运维，我想拿错密钥时得到明确「解密失败」而非坏 JSON， so 故障
   定位不歧义。

## Implementation Decisions

- AAD 绑定用途域而非会话 id（open 端解密前无从得知 id—— chicken-egg；
  域隔离已防跨域剪贴）。
- 单密钥环进程内口径（333 同）；双钥轮换由 EnvelopeCipher 原生支持
  （旧钥解密/新钥加密）。

## Testing Decisions

- seal→open 往返字段恒等；错主钥 open → DATA_CORRUPTION；非封缄
  输入 → DATA_CORRUPTION 带修法；同密文不同 AAD（333 消息域）解不开；
  isSealed 判定；篡改密文失败。

## Out of Scope

- cipher bean 化；ZIP 单会话加密；口令派生。

## Further Notes

- 新公共类型 `EncryptedSessionExport` 随轮 regenerate 快照 +
  api-surface.md 加行。
