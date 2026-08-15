# impl-127 — 消息读失败降级

**What to build:** buzhou.store.read-degrade=empty 时读失败降级空历史续聊（可感不静默）；缺省 off 行为不变。

**Blocked by:** None

**Status:** done

- [x] ReadDegradePolicy + ReadDegradeHolder（公共面）
- [x] BuzhouChatMemory loadHistory 统一路由（WARN + buzhou.stores.read-degraded 计数）
- [x] Store.readDegrade 属性（fail-fast）+ auto-config 初始化 bean 下发
- [x] 测试：OFF 上抛不变/EMPTY 空历史/写路径不受影响——core 312 绿
