# 1228 — R29：GuardAuditConfig fromGuardMap 解析全分支补测

> 来源：K 会话第 29 轮 = effort #1228（[T1871](../../.wayfinder/tickets/T1871-audit-config-shape.md) / [T1872](../../.wayfinder/tickets/T1872-audit-config-verify.md) / impl 931）。方法论：批次化延续——配置解析纯函数（Map 驱动确定性）的分支全补。

## Problem Statement

GuardAuditConfig.fromGuardMap（buzhou.guard.audit.* 子树解析，branch 80 missed/17%）承载审计开关、存储形态、容量、签名钥版本与密钥路径的全部容错解析——配置解析回归 = 审计面静默降级或启动失败。

## 目标

- GuardAuditConfigFromGuardMapTest（8 用例）：null guardMap 与非 Map audit 值回退默认；全字段往返（store trim+lower、容量、min-verify-version、key-dir、keys）；blank store/非法容量回退；负 min-verify-version 回退零；blank key-dir 忽略；非法 KeyFile 过滤（version≤0/路径缺失/blank）；public-key-path 可选缺省 null。

## 实现决策

- 纯函数 Map 驱动（无 Mockito）；先例：ConfigMaps（core 配置解析测试）。

## 测试决策

- 断言只对外部行为：解析产出的 record 字段值；不测私有 parseKeyFile。
- 验收门：定向绿 + GuardAuditConfig 分支 17%→92% 入账 + guard 全量绿。

## 兼容性

纯测试增量：主代码零变化、公共 API 面零变化、既有测试零改动。

## Out of Scope

- Jcs（84 missed）批次 6 候选。

## Further Notes

- 「解析函数 = 纯函数 = 最易全分支覆盖」——配置解析域是分支覆盖率性价比最高的战场。
