---
id: T954
title: 缓存 stale-while-revalidate 的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T953
created: 2026-09-13
---

## Question

stale 回程零等待？后台刷新真落表？刷新失败保旧值？单飞只触发一次？默认关零回归？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 2 轮）：① 注入 Clock 过 TTL 后、grace 内调用——立即返回旧值（staleServed=1），等待后台刷新完成后再调命中新值（hits 增、delegate 调用数=2）；② delegate 抛异常刷新——旧值保留、refreshFailures=1、后续仍回 stale；③ grace 窗外调用——硬过期重执行（现行为，misses 计数）；④ 并发 N 线程同 key stale 命中——delegate 重放恰 1 次（单飞）；⑤ 默认 wrap（grace=0）既有 TtlCachingToolCallback 用例零回归。`mvn -pl buzhou-core -am test` 全绿。
