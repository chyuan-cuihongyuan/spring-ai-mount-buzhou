# 149 — 未订阅流计数残留修复

**Parent:** spec 50 §C / [T180](../tickets/T180-unsubscribed-stream.md)

**Status:** done

- [x] stream() 惰性化：校验调用时 fail-fast；槽位获取/轮次开启移入 Flux.defer 订阅时
- [x] 语义钉住：顺序复订阅=重新开轮；在途并发订阅按单飞闸拒绝（既有）
- [x] 旧诚实边界注释改写；3 新测试（未订阅不占闸/复订阅重开轮/在途仍占闸）
- [x] 全仓 verify BUILD SUCCESS（惰性化影响面全回归）
