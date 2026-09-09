# Wayfinder Map — Buzhou 密钥扫描护栏（effort #400，D 会话第 1 轮）

> D 会话第 1 轮。勘察（2026-09-08）：PII 脱敏族（86/106/118/129/313）覆盖
> 身份信息五型 + 自定义规则，但**凭据类无任何覆盖**——`PiiDetector` 模式表
> 无 AKIA/ghp_/AIza/xox/JWT/PEM 私钥块；grep `secret|Secret` 在 guard 模块
> 仅命中 SecretLeases（core.exec，租约语义非扫描）。模型从工具结果或用户
> 粘贴中看到的凭据会原样进入上下文（观测/日志/export 族全量可见）并被
> 模型回显到出站工具参数（外发路径）——凭据一旦泄漏轮换成本远高于 PII。

## Destination

`guard.secret` 包（gitleaks 借鉴——pre-commit 扫描移动中的内容）：
`SecretType`（7 型有界枚举）+ `SecretScanner`（scan/redact，占位符
`[SECRET:TYPE]` 幂等）+ `SecretScanHook`（三缝统一 order 40：beforeTurn
输入 / beforeTool 出站参数 / afterTool 结果——均先于 PII 60/70 同层纵深）。
MASK 语义；opt-in 默认关；计数器 `buzhou.guard.secret.redactions`（tag type）。
装配：GuardModule.builder().secrets(...) + yml `buzhou.guard.secrets.{enabled,types}`。

## Notes

- 号段：spec 400 / T691–T692 / impl-373。
- 借鉴源：gitleaks（19k★）正则签名库思想 + truffleHog——只扫不阻断的
  观察口径 + 出站前拦截（pre-commit hook ≈ beforeTool）。
- 纪律：出站参数 MASK 的诚实边界（宿主自管凭据应走 env/工具配置，不经
  模型可见上下文——文档化）；有界枚举防基数失控。
- 勘察换题记录：原计划「模型回退链（LiteLLM）」勘察发现 FallbackChain
  族（含金丝雀/延迟感知/离群剔除/演练）已全量存在——弃，换本主题。

## Out of scope

- 熵值通用检测（gitleaks generic-api-key 的高误报面——真需求再议）；
- BLOCK 模式（HITL 危险工具守卫已覆盖按名拦截，凭据按型拦截另议）；
- 自定义密钥规则（CustomPiiRules 机制可镜像，需求出现再扩散）；
- 密钥命中 JSONL 导出（导出族扩散轮候选）。

## Tickets

- [x] [T691 SecretScanner 检测器](../tickets/T691-secret-scanner.md)
- [x] [T692 SecretScanHook 三缝 + 装配](../tickets/T692-secret-scan-hook-assembly.md)
