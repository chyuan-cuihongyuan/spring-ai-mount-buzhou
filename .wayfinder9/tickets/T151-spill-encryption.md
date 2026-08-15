---
Type: task
Status: closed
---
## Question

spill 落盘纯明文（DiskSpillStore.writeAtomically 直写 .spill，spec 02 已留位「静态加密未定」）：是否提供可配的落盘加密面？密钥来源、算法、向后兼容（已有明文文件）如何定？默认开关语义？

## Resolution

AFK 自决：是。新增 `SpillCipher`（buzhou-spill，JDK AES-256-GCM 信封，零新依赖；语义借鉴 Dify 凭据
AES-GCM）：wire 格式 = 魔法行 `BUZHOU-ENC-V1` + Base64(12B 随机 IV ‖ 密文+GCM tag)；`buzhou.spill.encryption-key`
（Base64 32 字节）配置即开、缺省关（零行为变化）；读侧魔法前缀探测——旧明文文件直通（向后兼容），
加密文件解密，GCM tag 验败（密钥错配/损坏）快速失败。meta 仍明文 JSON（sha256 为明文摘要，仅完整性
锚点）。StateEntry/DB at-rest 属部署层盘加密职责（runbook 记指引，不入代码）。产 spec 40 §A + impl-122。
