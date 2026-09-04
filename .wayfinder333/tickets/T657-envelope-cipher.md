---
Type: task
Status: closed
---
## Question

`EnvelopeCipher`（AES-256-GCM 信封 + keyId 自描述 + AAD 绑定 + 双钥
轮换）+ `EncryptingMessageStore`（载体透明往返 + 旧明文透传 + 重复包装
安全）。

## Resolution

done（2026-09-04）：impl-356；core.crypto 包落地。Cipher 七用例（往返/
AAD 换绑失败/篡改抛/轮换双钥/幽灵 keyId/坏钥红）+ Store 七用例（底层
只见载体/全字段还原/findById/旧明文透传/信封不再包装）绿。
