# 1404 — 会话 id 熵审计

> 来源：L 会话第 5 轮 = effort #1404（票 T2109 / T2110 / impl 1057）。**换题记录**：原题「指标基数守卫」勘察发现 spec 160/T513 已落地（tag 基数守卫 opt-in，Loki cardinality limit）——全撞换入 R19 备选题。借鉴：nanoid 熵计算器（bits = length × log2(alphabet)——alphabet/length 决定抗碰撞强度，README 内建 calculator）。

## Problem Statement

会话 id 由宿主 `spawn` 供给（runtime 兜底 UUID.randomUUID）——弱 id（时间戳串、"user123" 式可猜串、自增数字）无任何审计面：会话枚举/越权猜测/日志撞键风险静默。session 治理面（容量/迁移/租约）皆不问 id 质量。

## 目标

- `SessionIdEntropyAudit`（core/session，纯函数静态面，private 构造）：
  - `audit(String)` → 嵌套 `record Report(sessionId, length, alphabetSize, entropyBits, strength)`；
  - **下界估计口径**：从观测字符类保守推断字母表（小写 +26 / 大写 +26 / 数字 +10 / 非字母数字按去重符号计）——不猜生成器，只报一致可算的保守值（类越全熵越高，单调）；
  - 分档闭集 `enum Strength { INVALID, WEAK, MODERATE, STRONG }`：阈值常量 `WEAK_CEILING_BITS=64`（时间戳/自增串档）/ `STRONG_FLOOR_BITS=112`（≈UUIDv4 122 bits 量级）；空/空白 → INVALID；
  - `auditAll(List<String>)` → `record Summary(invalid, weak, moderate, strong)` 四桶批量面。

## 兼容性

纯函数零状态零 IO；只读不裁决（不拒绝 spawn——治理动作归调用方）；无配置项。

## Out of Scope

- 生成器接线（runtime 兜底已是 UUID——不强改宿主供给路径）。
- Shannon 频率熵（单样本无从统计频率；alphabet×length 下界是 nanoid 同款口径）。
- 会话 id 黑名单/模式规则（如保留前缀检查——另轴）。
