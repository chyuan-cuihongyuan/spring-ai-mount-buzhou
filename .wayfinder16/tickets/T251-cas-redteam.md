---
Type: task
Status: closed
blocked-by: T250
---
## Question

红队/单测：三 store CAS 语义（absent 建键 / 匹配换值 / 失配拒换 / stale 值换新日）；
配额并发钉住——多线程共享 store 递增最终计数 = N（不丢更新）；双「实例」（两
HookEnvironment 同 store）并发 turns/tool-calls/tokens 三维度竞态收敛；日翻越竞争只
重置一次；CAS 耗尽回退路径可注入伪 CAS 观察。

## Resolution

done（2026-08-29）：见 MAP Decisions 与 spec 56 对应节；实现/测试/文档随本 effort 提交入档。
