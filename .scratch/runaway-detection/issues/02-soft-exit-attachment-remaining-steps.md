# 02 — 软退出通道：AttachmentRenderer 注入「剩余步数」+ 软阈值事件

**What to build:** 达软阈值时经既有 Attachment 注入通道向模型注入「剩余步数预算」信号，让模型**主动收尾**而非被硬切。新增 `RunawayBudgetRenderer`（实现既有 `AttachmentRenderer`），经 `CompositeAttachmentRenderer` 折进 `InjectionViewProcessor` 的 `<system-reminder>` 注入块（插在近期原文之前，与事实块同位）；读 01 交付的轮次级步数计数，当 `remaining / limit < soft-threshold-ratio`（默认 0.2）时渲染「剩余步数预算：N/M，请尽快收尾并给出结论」，否则返回空；新增 `soft-threshold-ratio` 配置；每步刷新（memory advisor 每次模型调用重建视图 → renderer 每步触发）；软阈值**只注入不递减计数、不阻断**；`runaway.soft-threshold` 事件；注入字符计入既有 `buzhou.facts.max-inject-chars` 共享总量（与事实块共享同一份预算）。诚实标注切面次序：注入视图在 memory(+400) 构建、步数计数在 hook(+600) `beforeModel` 递增——本步注入用「上一步末」计数（一步滞后，模型看到「进入本次调用时的预算」，可接受）。

**Blocked by:** 01 — 复用其轮次级步数计数与 e2e 骨架

**Status:** ready-for-agent

- [ ] e2e：remaining 跌破 `soft-threshold-ratio` 时，模型所见 prompt 含「剩余步数预算」`<system-reminder>` 块（observing ChatModel 包装捕获 prompt，参照 `HookEndToEndTest`；或读 `injectionSnapshot(sid, turn)`）
- [ ] remaining 未跌破时不注入（合法长任务不受影响）
- [ ] 软退出提醒每步刷新（数值随步数递减更新）
- [ ] 软阈值只注入信号：计数照常递增、不阻断模型调用、不提前触发硬顶
- [ ] `runaway.soft-threshold` 事件出现，payload 含 counter/limit/remaining
- [ ] 注入字符受 `buzhou.facts.max-inject-chars` 共享总量约束（超额截断附指针，对齐事实注入既有规则）
- [ ] `soft-threshold-ratio` 仅在有 `per-turn.max-steps` 时生效；无步硬顶时不注入
