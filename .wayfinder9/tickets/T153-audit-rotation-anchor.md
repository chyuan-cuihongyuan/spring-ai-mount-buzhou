---
Type: task
Status: closed
---
## Question

SigningKeyRing.rotate() 仅内存原子切换——运行期轮换的新钥重启后不在环内，期间签名记录变「签名不可验」断链（真实 bug 面）；且链头 prevHash=SHA-256("") 常量、无外部锚定，DB 写权限者可整链重写、删尾不可检测：轮换持久化与外锚方案如何定？

## Resolution

AFK 自决：(a) 轮换「写而后切」——SigningKeyPersister 接口 + PemFileKeyPersister（v<version>.pem
约定命名原子落盘）+ SigningKeyRing.rotate 先落盘后切（失败中止 active 不变）+ PemFileKeyProvider.
scanDirectory 启动目录扫描（轮换新钥重启自动入环）+ signing.key-dir 配置（auto-config 同挂扫描与
持久化）。(b) 链外锚定——VerificationReport 增 headHash/anchorMatched（anchored() = intact 且锚一致），
verify 三参重载带外部锚点比对（删尾/重写可检测）；锚点安全属链外通道职责（诚实边界）。
KMS/Vault 集成与定时轮换显式出界（接口留扩展点 + runbook 记步骤）。产 spec 41 §A + impl-124。
