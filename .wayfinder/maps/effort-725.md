# effort #725 — 健康压权半开中点渐变（703 深化）

- 会话：G 会话 700 系第 26 轮 ｜ spec [725](../../../docs/spec/725-dampener-ramp.md) ｜ 票 [T1050](../tickets/T1050-dampener-ramp.md)/[T1051](../tickets/T1051-dampener-ramp-verify.md) ｜ impl625
- 借鉴：HAProxy slow-start（server 恢复后权重线性爬升惯例）

## 勘察（排重）

- 703 attach 原语 OPEN→地板、CLOSED→声明值一步到位；HALF_OPEN 当时不动作（维持地板）。慢恢复 provider 在 CLOSED 瞬间接满声明流量易二次跳闸。

## 决定

HALF_OPEN 分支补中点权重：weight=(floor+declared)/2——探测期即承载半量试探流量；CLOSED 恢复声明值不变。dampened() 读数扩展 stage（OPEN=full、HALF_OPEN=half）。语义仍是确定性三级阶梯（floor/half/declared），非时间窗渐变。

## 测试

OPEN→floor/HALF_OPEN→中点/CLOSED→声明值三段断言；奇数权重中点向下取整。

## 诚实边界
时间窗渐变（slow-start 秒级爬升）不做（需调度线程——两确定性档已覆盖主风险）。
