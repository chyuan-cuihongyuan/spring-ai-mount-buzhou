---
id: T2427
title: R39 泄漏金丝雀 yml 装配的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2426
created: 2026-09-15
---

## Question

N 会话第 39 轮：salt 走 yml 明文还是强制环境变量？

## Resolution

选 **yml 声明 + 文档指引环境变量注入**。强制（缺 env 即拒启）超出库的
边界判断——宿主的安全模型不同；spec 注明明文 salt 的强度折扣，选择权
归宿主（Spring 属性源天然支持 env 占位符 ${BUZHOU_CANARY_SALT}）。
