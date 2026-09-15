# 1229 — R30：Jcs（RFC 8785 自实现子集）规范化全分支补测

> 来源：K 会话第 30 轮 = effort #1229（[T1873](../../.wayfinder/tickets/T1873-jcs-shape.md) / [T1874](../../.wayfinder/tickets/T1874-jcs-verify.md) / impl 932）。方法论：批次化延续——**签名输入规范化 = 签名验证全线的前置合同**（规范化回归 = 签名验证全线失效）。

## Problem Statement

Jcs（guard 审计签名的输入规范化，RFC 8785 自实现子集约 175 行）零直接测试：字符串转义七字符+控制字符 \u00XX、对象键 UTF-16 字典序排序、整数-only 约束、非整数/不支持类型 fail-fast——全部分支从未执行。审计签名链的输入规范化回归 = 签名验证全线失效。

## 目标

- JcsCanonicalizationTest（10 用例）：标量规范形七种；非整数数值拒绝（Double/Float/BigDecimal）；不支持类型拒绝；对象键字典序排序（嵌套递归）；数组保序与空容器；集合内 null 序列化；canonicalizeJson 重排序归一；数组与字面量；非法 JSON 拒绝；非整数数值节点拒绝（包装消息含原始信息）。

## 实现决策

- 纯函数静态方法直测（无状态无 Mockito）；集合内 null 用 ArrayList/HashMap 构造（List.of/Map.of 拒 null 的容器语义差异即测试素材）。
- canonicalizeJson 的统一包装（「JCS 输入不是合法 JSON」）断言对包装消息——原始整数约束消息进 cause。

## 测试决策

- 断言只对外部行为：规范化输出字符串与异常类型/消息；不测私有 write/writeNode/writeString。
- 验收门：定向绿 + Jcs 分支 0 直接测试→97% 入账 + guard 全量绿。

## 兼容性

纯测试增量：主代码零变化、公共 API 面零变化、既有测试零改动。

## Out of Scope

- ResilienceAdvisor 深水区（891 行，独立深做轮）。

## Further Notes

- 「签名输入规范化」域与前序轮的共通教训：签名链的输入合同（规范化、序列化）是安全面最高频的静默失效点——测试即合同。
