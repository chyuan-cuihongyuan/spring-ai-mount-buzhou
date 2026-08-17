---
Type: task
Status: closed
---
## Question

effort #9 新能力（加密往返/单飞闸/审计轮换）的黄金轨迹（脚本化输入→事件序列断言）选哪三条？沿用 EventSequenceAssert 接缝。

## Resolution

AFK 自决：三条——G19 单飞闸（挂起工具在途→并发拒绝→收口→续轮）、G20 审计轮换持久化（v1 落链→
rotate 落盘→v2 落链→重启扫描全链可验+外锚删尾检出）、G21 spill 加密往返（密文/透明读/完整性锚/
旧明文兼容/装配闭环）。turn 生命周期非事件面（事件是机制级）——G19 以 inFlightTurns 等可观测
API 断言（口径修正入档）。产 spec 45 §A + impl-132。
