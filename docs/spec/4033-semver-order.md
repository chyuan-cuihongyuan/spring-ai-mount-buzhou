# Spec 4033 — 语义化版本序（effort #4033，R34）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6067–T6068，impl 2134）。
> 借鉴：Semantic Versioning 2.0.0（semver.org 优先级规则）。

## Problem Statement

版本协商的病：字符串字典序排版本（`1.0.10` < `1.0.9` 字典序
错、`1.0.0-rc` 被排到 `1.0.0` 之后错）——**机器性版本序面**
缺失。

## Solution

`SemVerOrder`（core/policy，嵌套 `Version` record 不另立面）：

- `parse`：semver.org §2 全量语法校验 fail-fast（三段数字、
  数字段禁前导零、pre-release/build 标识符合法字符、非空）；
- `compare`：§11 优先级规则——major/minor/patch 数值序；
  有 pre-release **低于**无 pre-release（1.0.0-alpha < 1.0.0）；
  标识符逐段比：纯数字段数值比（无溢出：先长度后字典序）、
  含字母段 ASCII 字典序、数字段 < 字母段；前缀全等时字段多者
  高；build 元数据**不参与**优先级（1.0.0+x ≡ 1.0.0）；
- `newerThan` 便捷读数。

## User Stories

1. 作为技能/插件版本协商作者，升级判定机器性正确——不因
   字典序误降级。
2. 作为审计作者，官方优先级链示例全链可重放。

## Testing Decisions

- semver.org §11 官方链示例全链有序（alpha < alpha.1 <
  alpha.beta < beta < beta.2 < beta.11 < rc.1 < 1.0.0）；
  build 元数据等价；数值段 10 > 9 与字母序反例；数字段 <
  字母段（1 < alpha）；非法版本五型 fail-fast（缺段/前导零/
  空 pre/非法字符/v 前缀）。

## Out of Scope

- 不做版本范围表达式（`^1.2.3` / `~1.x` NPM 语义——留后）；
- 不做版本升级建议/变更分类（conventional-commits 推导）。

## Further Notes

- 与 CrockfordBase32（人类可转录编码）同族不同面：标识编码
  vs 标识排序。Wave 6 第四件。
- 里程碑：34/50。
