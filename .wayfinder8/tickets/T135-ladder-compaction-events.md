---
Type: task
Status: open
---
## Question

压缩梯子事件化（effort #7 fog）：10% 步进加压（0.7→1.0）只发首轮事件，梯子级不可观测。补齐口径？

## Resolution

AFK 自决：梯子每级实际折入都通知。InjectionViewProcessor 梯子循环内的 compact 调用后 notifyCompaction（现 notify 已在主路径，梯子路径漏）——加于每级；payload 增 `evictRatio`（当前级比例），消费方可区分梯子级。产 spec 38 §A + impl-108。
