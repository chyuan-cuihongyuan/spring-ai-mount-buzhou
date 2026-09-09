# Wayfinder Map — Buzhou 工具混沌注入（effort #322，C 会话第 23 轮）

> C 会话第 23 轮。韧性机制已一纵深（熔断/重试预算/舱/泳道/错误预算……），
> 但都是"被动等真故障"——没有主动验证它们真的会动的面。Netflix Chaos
> Monkey 思想：按概率注入延迟/故障，让韧性机制在平时就被真枪实弹演练。

## Destination

`ChaosMonkeyHook`（core.exec，order 235 先于熔断 240）：beforeTool 按概率
注入——延迟（Thread.sleep 固定毫秒）与故障（HookResult.block 结构化错误
标记——131/321 同语义，模型可读可改道）。概率源可注入（测试确定性）；
运行时开关 setEnabled（混沌会话随手启停）；include 工具清单（空=全量）；
计数器。yml `buzhou.chaos.*` 默认关。

## Notes

- 号段：spec 322 / T635–T636 / impl-345。
- 借鉴：Netflix Chaos Monkey（概率故障 + 运行时开关 + kill-switch 默认关）。

## Decisions so far

- 故障注入走 block（结构化标记）不走抛异常：hook 契约内表达，模型可读
  可改道——与熔断拒的词汇族一致。
- 延迟注入是同步 sleep（hook 链同步模型内诚实——不假装异步）。
- 默认 enabled=false；装配后运行时可 setEnabled 翻转（演练窗口）。

## Out of scope

- 结果篡改注入（replaceResult——需 afterTool 深钩，后续按需）；进程级
  资源混沌（CPU/内存）；定时全局袭击（类 Chaos Monkey 调度器——宿主接）。

## Tickets

- [x] [T635 ChaosMonkeyHook + 属性 + 装配](../tickets/T635-chaos-hook.md)（impl-345）
- [x] [T636 回归与收口](../tickets/T636-chaos-close.md)（impl-345）
