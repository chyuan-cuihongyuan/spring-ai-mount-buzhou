# Spec 401 — 提示词注册表（effort #401）

> wayfinder map：`.wayfinder/maps/effort-401.md`（T693–T694）。D 会话第 2 轮。

## Problem Statement

提示词是 Agent 行为变更的最大单点，但无版本化管理：宿主改提示词只能改
代码重发——无版本号、无标签、无回滚指针、无「当前生效哪一版」的单一
事实源；多人协作时「上次改了什么」靠 git 考古与口头对齐。

## Solution

`core.prompt` 包（Langfuse prompt management 借鉴——版本 + 标签双轴）：

- **`PromptVersion`**：不可变 record（name, version, body, note,
  publishedAt）——发布即快照，永不改写。
- **`PromptRegistry`**（接口，`LATEST = "latest"`）：
  - `publish(name, body, note)`：per-name 单调递增版本；`latest` 标签
    自动重指新版本；
  - `label(name, label, version)`：标签重指（晋级/回滚同一动作——标签
    恒指一个版本）；未知 name/version fail-fast（IllegalArgumentException）；
  - `resolve(name)`（默认 latest）/ `resolve(name, label)` /
    `resolveVersion(name, version)`（钉版——复现历史行为用）；
  - `labels(name)`（标签→版本指针表）、`versions(name)`（升序全史）、
    `names()`。
- **`InMemoryPromptRegistry`**：进程内实现（ConcurrentHashMap + per-name
  同步）；标签语义归宿主（registry 不解释标签名——production/staging
  是约定不是机制）。
- 装配：`buzhou.prompt.templates[{name, body, label}]` yml 播种——同
  name 同 body 幂等跳过（**重启不掀版本**）；声明 label 自动指向该名
  当前最新；bean 恒在（未配置 = 空注册表，零行为变化）。

## User Stories

1. 作为提示词工程师，我想 publish 即得单调版本号，所以 每次变更有
   不可改写的历史记录。
2. 作为运维，我想把 production 标签指回旧版本，所以 行为回滚不需要
   改代码重发。
3. 作为评测作者，我想按版本号钉取历史提示词，所以 复现历史行为时
   不受标签移动影响。
4. 作为宿主，我想 yml 声明提示词且重启幂等，所以 版本号不因重启
   空转膨胀。

## Implementation Decisions

- 标签存注册表态（非版本快照字段）——指针移动不产生新版本（Langfuse
  同口径：label 是指针不是快照属性）。
- yml 幂等口径 = body 全等（note 不参与——重启 note 变化不掀版本，
  诚实边界文档化）。
- 进程内先行：跨实例共享（DB/Redis）等真需求出现再扩 SPI。

## Testing Decisions

- publish 单调 + latest 自动重指；
- label 晋级/回滚 + 钉版 + 未知名 fail-fast；
- yml：播种 + 同 body 幂等（同名两条同 body 只一版）+ label 指针 +
  未配置空注册表。

## Out of Scope

- 持久化注册表；publish/label 事件流与面板；模板变量渲染；A/B 流量
  分配。

## Further Notes

- 新公共类型 `PromptVersion` / `PromptRegistry` / `InMemoryPromptRegistry`
  / `BuzhouPromptProperties` 随轮 regenerate 快照。
