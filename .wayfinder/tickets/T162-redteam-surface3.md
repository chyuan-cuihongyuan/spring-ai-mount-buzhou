---
Type: task
Status: closed
---
## Question

新攻击面对抗：密钥错配（换钥后读旧文件）/审计链篡改（重写/删尾）在加密与外锚下的行为是否可被确定性钉住？红队用例形态（观察档/断言档）如何定？

## Resolution

AFK 自决：四用例观察档（examples StaticSecurityAdversarialTest，确定性对抗、替身域外）——密钥错配
快速失败/密文位翻转 GCM 检出/记录改写断链定位/删尾与整链重写的签名+外锚检测边界（纯哈希降级模式
盲区由外锚补检，诚实钉住）。转 nightly 硬门出界（先观察）。产 spec 45 §B + impl-133。
