# 1510 — ConfigMaps indexed 属性数字键归一

> 来源：M 会话第 12 轮 = effort #1510（impl 1113）。R9（spec 1508）副产出发现的通用修复：properties 源 indexed 列表键静默失效。

## 背景

`ConfigMaps.sub`（装配层配置桥）用 `Binder.bind(prefix, mapOf(String,Object))`——.properties/命令行/env-var 源的 indexed 属性（`key[0].f=v`）被 Binder 绑成 `{key={0={f=v}}}`（Map 形态）而非 List。一切经 `fromYml(Map)` 以 `instanceof List` 消费列表键的模块（guard dangerous-tools、tools 开关矩阵、canary 权重等）在这些源下静默失效（YAML 文件源正常）。

## 目标

- `normalizeValue` 的 Map 分支加数字键归一：键集非空且全为十进制数字 → 按数值序转 List（避免字典序 10<2 乱序）；任一非数字键保持 Map；
- 嵌套递归归一（列表元素内的 indexed map 同样处理）。

## 兼容性

YAML 文件源（既有 List）零变化；数字键 map 在 YAML 语义不存在、properties 的 `[i]` 恒为列表语义——归一安全。properties 源下列表键从静默失效变为正确解析（缺陷修复）。
