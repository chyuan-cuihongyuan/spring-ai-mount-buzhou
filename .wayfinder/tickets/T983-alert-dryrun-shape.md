---
id: T983
title: 告警规则 dry-run 的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

告警规则集变更（新规则/改阈值）只能上线实弹验证——误配（mechanism 拼错、for 窗不合理）直接打到通知通道。K8s admission dry-run / argo sync --dry-run 语义怎么映射到 AlertRuleEngine？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 17 轮 = effort #716 / spec 716 / impl 519）：AlertRuleEngine 增 `dryRun(Instant now)`——**纯只读推演**：对每条规则读当前健康面，返回 `DryRunReport`：① `wouldFire`（DOWN 且 for=0 或窗口已满且未在 firing——若实弹将触发）；② `wouldRecover`（firing 中但已 UP）；③ `pending`（DOWN 且窗口未满——含 downFor 与剩余时长）。**三不承诺**：不改 downSince/firing 状态机、不通知 listener、不发指标（dry-run 零副作用——规则演练不污染实弹状态）。复用 AlertFiring 形态（通知通道可复用消费逻辑）。装配面（控制台/端点暴露）留后续轮。借鉴 k8s ValidatingWebhook dryRun、argo sync --dry-run。
