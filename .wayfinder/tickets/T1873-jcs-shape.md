---
id: T1873
title: R30 选题——Jcs（RFC 8785 自实现子集）规范化全分支补测
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 30 轮：Jcs（84 missed，零直接测试，审计签名的输入规范化执行面）如何补测？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 30 轮 = effort #1229 / spec 1229 / impl 932）：

1. **补测面（10 用例）**：标量规范形（null/string/boolean/int/long/short/BigInteger）；非整数数值拒绝（Double/Float/BigDecimal）；不支持类型拒绝；对象键 UTF-16 字典序排序（嵌套递归）；字符串转义全覆盖（JSON 强制七字符+控制字符 \u00XX）；空容器与集合内 null；canonicalizeJson 重排序与归一；非法 JSON 拒绝；非整数数值节点拒绝（包装消息）。
2. **形态**：纯函数静态方法直测（无状态无 Mockito）；先例：config 解析测试族。
3. **边界**：不改主代码；签名链集成面由 AuditChain* 既有套件覆盖。
