# Wayfinder Map — Buzhou 消息静态信封加密（effort #333，C 会话第 34 轮）

> C 会话第 34 轮。库内零加密（grep 全模块无 Cipher）：消息、推理、工具
> 实参明文落库（JDBC/Redis/归档）。共享存储/托管 DB 场景下库管看得见
> 全部会话内容。Vault transit / AWS KMS envelope encryption 的解法：
> 应用层信封加密——密钥不出进程，存储只见密文；AAD 绑定标识防剪贴；
> 双 keyId 轮换平滑换钥。

## Destination

`EnvelopeCipher`（AES-256-GCM：信封格式 `buzhou:v1:<keyId>:<iv+ct>`、
AAD=消息标识绑定防剪贴、当前钥加密+前代钥解密的轮换面）+
`EncryptingMessageStore` 装饰器（载体消息透明往返：结构路由字段明文、
内容全密文；非载体旧数据透传兼容）+ yml 装配
（`buzhou.security.message-encryption.{master,previous}-master-key`，
BeanPostProcessor 重建 BuzhouStores——宿主零改动）。

## Notes

- 号段：spec 333 / T657–T658 / impl-356。
- 借鉴源：HashiCorp Vault transit / AWS KMS envelope encryption。
- 纪律：未配 master-key = 零变化（BPP 不装配）；篡改宁可炸不可静默
  （GCM 认证失败上抛——完整性优先于可用性）；轮换=写新钥读双钥。

## Decisions so far

- 序列化手写归一映射（createdAt→epochMillis 等）——不依赖 jackson-jsr310
  模块在场；metadata 值经 JSON 往返归一（文档化）。
- keyId = 主钥 SHA-256 前 8 字节 hex——稳定派生，信封自描述可路由解密。
- 载体消息保留 id/sessionId/turnSeq/seqInTurn/createdAt 明文（排序/检索
  路由需要），其余全密文；role 以 USER 占位（真 role 在密文内）。

## Out of scope

- SummaryStore/SessionStateStore 加密（模式验证后扩散——同 BPP 通道）；
- KMS/外部密钥服务集成（KeyProvider SPI 另立）；密文检索/按内容查询。

## Tickets

- [x] [T657 EnvelopeCipher + EncryptingMessageStore](tickets/T657-envelope-cipher.md)
- [x] [T658 yml 轮换面 + BPP 装配 + 收口](tickets/T658-encryption-assembly.md)
