# 140 — 流取消分类计数与慢滴流累计上限

**Parent:** spec 46 §B / [T171](../tickets/T171-stream-cancel-cumcap.md)

**What to build:** 流终止原因分类计数 `buzhou.stream.cancelled{reason=client|deadline|guard}`（预注册有界枚举）；
新增流累计时长上限（`buzhou.session.stream-total-timeout`，缺省 10m，≤0 关闭）——慢滴流到点以标记异常
onError 终结并按 deadline 计数，复用既有 failTurnOnce 收尾链路。

**Blocked by:** None — can start immediately

**Status:** ready-for-agent

- [ ] 三路分类各计数：订阅者 cancel（doFinally CANCEL）= client；流超时/累计超限错误（doOnError）= deadline；beforeTurn 护栏拦截 = guard
- [ ] 累计上限：takeUntilOther(delay(cap)→标记异常) 语义；标记异常为 core 内部类型、消息注明「流累计时长超限」；既有相邻信号 timeout 语义不变、先到者生效
- [ ] `stream-total-timeout` 配置（Duration，缺省 10m，≤0 显式关闭）fail-fast 校验 + HarnessAssembler 传入 + 兼容构造保留
- [ ] 慢滴流测试：每 tick 滴一字伪流 + 200ms 上限 → 到点标记异常终结、deadline 计数、onTurnError 通知
- [ ] 关闭开关（≤0）回归：长流不被截断；client/guard 计数互不串扰
- [ ] buzhou-core `mvn verify -am` 全绿 + 行为变更（缺省 10m 上限）记入待办（T186 api-surface）
