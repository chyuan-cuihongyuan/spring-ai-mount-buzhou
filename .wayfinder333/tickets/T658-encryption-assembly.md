---
Type: task
Status: closed
---
## Question

yml 面（master-key/previous-master-key，Base64 AES 钝 fail-fast）+
BeanPostProcessor 重建 BuzhouStores（只换 messageStore 槽）+ README/
快照收口。

## Resolution

done（2026-09-04）：impl-356；BuzhouMessageEncryptionProperties + 静态
BPP bean（ConditionalOnProperty master-key——未配不装配零变化）；BPP
经 Environment 读钥（早于属性 bean 就绪的诚实顺序）。装配三用例
（声明即换装+容器内往返/未配零变化/坏钥启动红）绿；快照 regenerate
+3 型；README 纵深 IV 加行、覆盖门绿。
