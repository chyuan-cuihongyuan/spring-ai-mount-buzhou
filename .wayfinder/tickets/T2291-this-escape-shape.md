---
id: T2291
title: 构造器 this 逃逸修复 + 三裁定（六-7/五-2/六-2 指纹）的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by:
created: 2026-09-15
---

## Question

M 会话第 23 轮：六-7（构造器启线程 this 逃逸）、五-2（@Bean 裸读 Environment）、六-2 剩余（双轨规范化 JSON）如何处置？

## Resolution

**用户常设授权 AFK（可推翻）**

① 六-7 AsyncObservabilityPipeline：构造器内 unstarted 后**立即 start**（this 逃逸真实——非 final 类子类字段未初始化时 drainLoop 可读默认值）——改惰性启动（BufferedEventDispatcher 懒创建先例）：CAS ensureDrainStarted 首事件触发，close 的 join 加 isAlive 防御（零事件零线程，close 对 unstarted 线程只 interrupt 不 join）；DbToolSetProvider 裁定不整改（scheduleWithFixedDelay 首跑延迟 pollInterval——逃逸窗口理论性，实际定时点构造早已完成）；
② 五-2 裁定：CLAUDE 规约补追认边界——三键以内简单 opt-in 装配允许 getProperty 直读（10 处既成先例），复杂配置面必须 record；
③ 六-2 指纹裁定：ArgumentFingerprint（Jackson 排序，授权域）与 Jcs（RFC 8785，审计域）**不统一**——指纹是 auth state key 成分，统一即改值 = 部署升级后既有授权失效；两处 Javadoc 交叉注记替代。
