---
Type: task
Status: closed
blocked-by: T173-turn-feedback.md, T176-shadow-fork.md, T177-model-pool-quota.md
---
## Question

反馈伪造（越权/伪造 turnSeq 反馈他人会话——应拒）/ shadow 泄漏（shadow 结果不得回注）/ 配额绕过（窗口边界突发）/ 权重漂移（同会话漂移检测）；观察档 + 检测边界诚实钉住。

## Resolution

spec 51 §B / impl-151 落地：redteam/README.md 增 effort#10 观察档（反馈伪造——rateTurn 会话
实例方法跨会话伪造构造上不可能、合法会话内虚假反馈属部署面；shadow 泄漏——输出零回注
（G24 承载）、prompt 内容外流到 shadow 提供方以同信任域为部署前提；配额绕过——跳级唯一性；
金丝雀漂移——多轮粘住、配置变更重启后漂移一次属可接受运维窗口）。确定性对抗 2 测试
（每轮每候选恰一次触达 ×2 轮；20 会话含失败回退轮的粘性）。resilience 全绿。
