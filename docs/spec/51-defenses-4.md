# Spec 51 — 防线第四批：黄金 F / 红队四 / perf 哨兵（effort #10）

> effort #10 第六篇。§A：黄金轨迹 F（T181）；§B：红队对抗四批（T182）；§C：perf 哨兵四批（T183）。
> 沿用 spec 32/38/39/45 防线哲学：机制行为的「脚本化输入 → 事件/端点序列断言」回归；
> 红队观察档 + 检测边界诚实钉住；perf 哨兵 10 倍宽幅硬顶。

## §A 黄金轨迹 F（T181 / impl-150）

- **G22 反馈捕获轨迹**：chat 一轮 → rateTurn(boolean false) → 事件序列断言
  [turn.feedback{turnSeq=1,type=boolean,value=false}] + state store 落键（scanByPrefix）。
- **G23 金丝雀稳定轨迹**：权重 1:9 配置下同一会话多轮——首选粘住（canary.selected 恰一次、
  回复同源）；跨会话允许分流（不锁死算法，只钉粘性）。
- **G24 shadow 隔离轨迹**：shadow 启用 + 主模型成功——用户回复来自主模型、shadow.compared
  事件到达且主链路无 fallback.switched（隔离性：探测零回注）。

## §B 红队对抗四批（T182 / impl-151）

- **反馈伪造**：伪造他人会话反馈——rateTurn 是会话实例方法（无跨会话面），观察点转为
  「越权不可能由构造保证」+ 非法输入全拒（六路校验已钉）；观察档记录该面收敛理由。
- **shadow 泄漏**：shadow 结果不得回注用户（回复与 shadow 输出解耦断言）；shadow 失败零影响。
- **配额绕过**：候选限流闸拒绝后不可绕到下一级重复调用同一候选（跳级唯一性）。
- **金丝雀漂移**：同会话多轮不得漂移到不同目标（粘性 = 漂移检测面）。
- 检测边界诚实钉住：以上为框架级对抗面；提示注入/越权会话属部署面（沿 spec 22/38 边界）。

## §C perf 哨兵四批（T183 / impl-152）

- TTFT 打点开销：带打点 vs 不带打点的流式吞吐 delta（宽幅 10×；打点为纳秒级 nanoTime 两次 +
  一次 map 写——预期近零）。
- rateTurn 写入开销：单次反馈（state put + 事件）≤ 10ms 量级。
- 候选限流闸开销：带闸 vs 不带闸的降级路径延迟 delta（预检两次 map 查找——预期近零）。
- shadow 提交开销：主路径成功返回延迟 delta（提交即返回——预期 <1ms）。
- baseline 落档 docs/perf/baseline.md（第四批四哨兵）。
