---
id: T968
title: HookTiming 滚动 max 读面的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T967
created: 2026-09-13
---

## Question

滚动 max 真衰减（过窗归零）？窗口内样本保留？生命周期 max 不受影响？未开启聚合零开销？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 9 轮）：① 注入时钟：record 峰值后推进过窗——windowedMax=0（诚实）；未过窗=峰值；新低样本后=max(窗口内新样本)；② 生命周期 max 不变（stats() 口径零变化）；③ 桶翻转覆盖（>BUCKETS 个桶间隔后旧桶正确重置）；④ 未开启 Holder 时零镜像（既有用例零回归）。`mvn -pl buzhou-core -am test` 全绿。
