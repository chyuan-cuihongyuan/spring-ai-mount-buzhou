# Spec 400 — 密钥扫描护栏（effort #400）

> wayfinder map：`.wayfinder/maps/effort-400.md`（T691–T692）。D 会话第 1 轮。

## Problem Statement

PII 脱敏族覆盖身份信息，但凭据类零覆盖：模型从工具结果或用户粘贴中看到的
API key / token / 私钥会原样进入上下文（观测、日志、export 族全量可见），
并可能被模型回显到出站工具参数（外发路径）。凭据泄漏的轮换成本远高于
PII，且泄漏通道（prompt 转储、JSONL 报表）恰是观测体系的日常面。

## Solution

`guard.secret` 包（gitleaks 借鉴——在内容移动的缝上扫描）：

- **`SecretType`**（7 型有界枚举）：AWS_ACCESS_KEY（`AKIA[0-9A-Z]{16}`）、
  GITHUB_TOKEN（`gh[pousr]_[A-Za-z0-9]{20,}`）、GOOGLE_API_KEY
  （`AIza[0-9A-Za-z_-]{35}`）、SLACK_TOKEN（`xox[baprs]-[0-9A-Za-z-]+`）、
  OPENAI_STYLE_KEY（`sk-[A-Za-z0-9_-]{30,}`）、JWT（三段 `eyJ…`）、
  PRIVATE_KEY_BLOCK（`-----BEGIN … PRIVATE KEY-----` 块）。
- **`SecretScanner`**：`scan(text)` → 命中列表（type + 区间）；
  `redact(text)` → `[SECRET:TYPE]` 占位符替换；幂等（已含占位符前缀
  `[SECRET:` 不再处理）；无命中零改写（引用等）。
- **`SecretScanHook`**（BuzhouHook，MASK 语义、opt-in 默认关）三缝统一
  order 40（单 hook 单 order——三缝同位，均先于 PII 两侧 60/70 与 HITL
  指纹 300）：
  - `beforeTurn`：用户粘贴凭据不进 prompt 与观测；
  - `beforeTool`：出站工具参数值脱敏——外发前拦截（gitleaks pre-commit
    同位）；**诚实边界**：宿主自管凭据应经 env/工具配置注入，不走模型
    可见上下文——开启即假设该纪律成立；
  - `afterTool`：工具结果回灌上下文前脱敏。
- 计数器 `buzhou.guard.secret.redactions`（tag type——7 值有界枚举）。
- 装配：`GuardModule.builder().secrets()` / `.secrets(types)` + yml
  `buzhou.guard.secrets.{enabled,types}`（List/CSV，未知类型 fail-soft 忽略
  ——与 PII types 同口径）。

## User Stories

1. 作为安全负责人，我想用户误粘贴的 API key 在进模型前被占位符化，
   所以 凭据不进 prompt、观测与日志。
2. 作为安全负责人，我想模型从工具结果回显的凭据在外发工具参数前被
   脱敏， 所以数据外发路径不成为凭据泄漏通道。
3. 作为运维，我想密钥命中按型计数， 所以泄漏面趋势可见（哪个类型
   常出现=哪类凭据管理最松）。

## Implementation Decisions

- MASK 不 BLOCK：与 PII 族同语义（改写保持可读、不污染熔断/预算）；
  按型拦截交给 HITL 危险工具守卫的组合。
- 三缝一个 hook：同检测器同配置三处复用，避免三个类漂移。
- JWT 中段要求 base64url 形状（两 `.` 分隔）降低误报；OPENAI_STYLE_KEY
  长度下限 30 防误伤普通 `sk-` 前缀词。

## Testing Decisions

- 检测器：每型正/负样本 + 幂等 + 无命中引用等；
- hook：三缝各自改写断言（beforeTurn replaceInput / beforeTool
  replaceArguments / afterTool replaceResult）+ 无命中零改写；
- 装配：builder 程序面 + yml enabled/types 解析（镜像 PiiYml 用例）。

## Out of Scope

- 熵值通用检测（高误报面另议）；BLOCK 模式；自定义密钥规则；
  命中 JSONL 导出（导出族扩散候选）。

## Further Notes

- 新公共类型 `SecretType` / `SecretScanner` / `SecretScanHook` 随轮
  regenerate 快照。
