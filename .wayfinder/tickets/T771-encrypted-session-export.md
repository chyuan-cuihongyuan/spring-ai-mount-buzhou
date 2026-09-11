---
Type: task
Status: closed
---
## Question

`EncryptedSessionExport.seal/open`：封缄标记头+EnvelopeCipher AES-GCM
+AAD 用途域绑定；坏标记/坏密文 DATA_CORRUPTION fail-fast 带修法。

## Resolution

done（2026-09-12）：impl-413；往返/错钥/非封缄/跨域 AAD 用例绿。
