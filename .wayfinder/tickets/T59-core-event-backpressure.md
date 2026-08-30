---
id: T59
title: core · 事件分发背压与线程卫生
type: grilling
status: closed
assignee: ""
blocked-by: [T55]
created: 2026-08-14
---

## Question

同步内联事件分发如何升级为有界异步？需裁决：① BoundedEventBus 形状（容量/水位/溢出策略枚举 DropOldest|Block(pushTimeout)|DeadLetter，Netty 水位 + Akka 死信语义）；② 持久化类监听器 vs 遥测类监听器的默认策略分派；③ 丢弃可见性（计数指标 + 低频汇总事件）；④ 订阅者异常隔离（绝不向 chat()/close() 传播）；⑤ 兼容性——现有 SessionEventListener 同步语义如何过渡（默认保持同步、总线为 opt-in？还是默认切总线）；⑥ 线程命名规范（buzhou-<role>-<短哈希>、uncaughtExceptionHandler、DbPolicyConfigProvider 静默吞异常修复）。

## Resolution

**ratify（用户常设授权，可推翻）→ spec 13 §core-4**：默认保持同步分发（兼容）但补逐监听器异常隔离 + 计数；opt-in buffered 模式（有界容量 + DropOldest/Block(pushTimeout) 溢出策略）；丢弃可见（计数器 + 低频汇总事件，Akka 死信语义）；BuzhouThreadFactory（buzhou-<role>-<seq> + uncaughtExceptionHandler）覆盖全部线程创建点；DbPolicyConfigProvider 吞异常修复（WARN + 指数退避）。
