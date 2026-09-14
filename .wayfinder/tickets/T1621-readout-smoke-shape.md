---
id: T1621
title: J 系读面统一契约冒烟轮的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1619
created: 2026-09-15
---

## Question

J 会话第 83 轮：读面谱系自身的质量增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：J 系 20+ 个静态 stats() 读面缺**统一契约冒烟**——每读面可调用、组件非负、reset 生效三项元性质无一处集中验证（各轮仅验证各自语义）。读面谱系的谱系（meta-readout）。

形状裁决：starter 模块新增 `ReadoutContractSmokeTest`（聚合模块天然跨模块 classpath）——显式清单驱动（J 系 R46–R77 全部 15 个带 stats() 的类）反射调用 stats()：①组件全非负 ②resetForTest() 后全归零 ③重复调用稳定。清单显式维护=新读面登记纪律的落点。纯测试轮零生产改动。
