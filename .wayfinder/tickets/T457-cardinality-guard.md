---
Type: task
Status: closed
---
## Question

tag 基数守卫：装饰器封顶 + 越限折 __overflow__ + 守卫面计数。

## Resolution

done（2026-08-30）：impl-278；`metrics/TagCardinalityGuard`（wrap 两形态 +
counter/timer/gauge 三面 + 指标名空间 512 封顶 + 畸形键值透传）+ 红队 7 例。
