---
Type: task
Status: open
---
## Question

SigningKeyRing.rotate() 仅内存原子切换——运行期轮换的新钥重启后不在环内，期间签名记录变「签名不可验」断链（真实 bug 面）；且链头 prevHash=SHA-256("") 常量、无外部锚定，DB 写权限者可整链重写、删尾不可检测：轮换持久化与外锚方案如何定？
