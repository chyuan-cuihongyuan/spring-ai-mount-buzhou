---
id: T2299
title: 配置错误显形双小项（hook 重名/observer 重复注册）的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by:
created: 2026-09-15
---

## Question

M 会话第 27 轮：hook 重复名与 observer 同实例重复注册如何显形？

## Resolution

**用户常设授权 AFK（可推翻）**

① HookChain 构造期重复名检测：同名 hook（order 平局时 name 比较无区分——派发序不稳定 + stats/禁用按名对位歧义）WARN 显形不炸装配（治理归调用方决断；Kong 插件重名诊断思想）；
② DefaultSessionAssemblyContext.addObserver 同实例幂等去重（contains 检查——双份通知是装配错误信号，静默双计污染观察者读面；listener 域 addEventListener 的 List.add 不去重维持——监听者常为 lambda 多实例，identity 去重无意义且 O(n) 检查白付）。
