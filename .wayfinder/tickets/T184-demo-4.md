---
Type: task
Status: closed
blocked-by: T173-turn-feedback.md, T175-weighted-canary.md, T176-shadow-fork.md
---
## Question

TTFT 观测演示 / rateTurn 反馈演示 / 金丝雀权重切换演示 / shadow 对照演示（各一 examples 场景）。

## Resolution

spec 51 续 / impl-153 落地：四演示——TTFT 观测（宿主从 MODEL_CALL span 属性直读 ttft.ms/
tpot.ms，dashboard 回放同源）/ rateTurn 反馈（turn.feedback 事件 + core.feedback 导出段随
SessionExport）/ 金丝雀权重（3:1 配置 20 会话分流 5-20 宽幅 + 粘住 + 首选事件）/ shadow
对照（主模型答案 + shadow.compared deltaMs 异步到达）。4 新测试绿。
