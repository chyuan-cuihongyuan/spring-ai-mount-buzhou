# effort #842 — HITL 认证决策分布

- 会话：H 会话 800 系第 43 轮 ｜ spec [842](../../../docs/spec/842-auth-decision-stats.md) ｜ 票 [T1185](../tickets/T1185-auth-decision-stats.md)/[T1186](../tickets/T1186-auth-decision-stats-verify.md) ｜ impl595
- 借鉴：Keycloak required-actions 决策观测思想扩散（keycloak/keycloak ≈29K）

## 勘察（排重）

- GuardAuthApi：approve/revoke 执行——无决策分布。
- AuthTtl：TTL 语义（once/session）——无统计。
- grep -i `auth.*stats|decision.*count`：无命中。

## 决定

`AuthDecisionStats`（guard.hook，synchronized 记账）：record(Outcome)——五态 GRANTED/DENIED/EXPIRED/CONSUMED/UNKNOWN 闭集计数+占比；snapshot 占比降序平局声明序；null 忽略；空真。喂点=GuardAuthApi/确认 hook 装配侧。

## 测试

计数+占比 0.6 降序/凭据管理类推导三例/null 忽略+空真——3 例全绿。

## 诚实边界

喂点手动（不改 approve/校验行为）；五态语义归 GuardAuthApi（本类不解释）；进程内存有界。
