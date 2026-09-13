# effort #811 — 断路器 crash-loop 检测

- 会话：H 会话 800 系第 12 轮 ｜ spec [811](../../../docs/spec/811-circuit-crash-loop.md) ｜ 票 [T1123](../tickets/T1123-circuit-crash-loop.md)/[T1124](../tickets/T1124-circuit-crash-loop-verify.md) ｜ impl564
- 借鉴：k8s CrashLoopBackOff（kubernetes/kubernetes ≈115K star）——反复崩溃进退避、成功运行才复位

## 勘察（排重）

- CircuitTransitionJournal（702）：变迁留痕——无「反复跳闸」状态判定。
- ModelOutlierEjection：供应商逐出（多模型池视角）——单模型循环状态缺位。
- grep -i `crashloop|looping|backoff`：无命中（RetryBudget 是重试预算不同族）。

## 决定

`CircuitCrashLoopDetector`（resilience 根包）：recordOpen 滑窗计数（windowMillis 内 ≥minOpens）→ looping 闩锁态（窗口滑过不自动解除——k8s 对齐）；recordRecovery（HALF_OPEN→CLOSED 喂点）显式清除+清窗；loopsDetected 只在「进入闩锁」时 +1（反复炸反复计）；模型封顶 32+truncated；minOpens≥2 fail-fast（「反复」语义底线）。旁路喂点同 702 journal 挂点。

## 测试

阈值转闩+闩锁跨窗不解+恢复清除清窗+loopsDetected=1/窗口滑出不闩 opensInWindow=1/恢复后再闩 loopsDetected=2/模型独立+封顶 32+truncated/null 忽略+快照典序/参数 fail-fast——6 例全绿（首跑测试误用 minOpens=1 与「反复」语义矛盾——修测试）。

## 诚实边界

旁路读数不阻止试探（是否停试归路由策略域）；OPEN 喂点由装配侧接（702 同模式）；窗口滑出不解除闩锁是刻意的（解除唯恢复路径——与「滑动窗口告警」族的区别即在此）。
