# Wayfinder Map — M 会话 1500 系：借鉴高价值开源项目的 50 轮自迭代（effort #1500 总图）

> **M 会话**（2026-09-15 启动）：继 C（300）/ D（400）/ E（500）/ F（600）/ G（700）/ H（800）/ I（900）/ J（1000）/ K（1200）/ L（1400）之后的第十一条自迭代线。
> **号段裁决（号段声明先行）**：M 会话占用 spec **1500–1549**、票 **T2251–T2350**（每轮 2 张：shape + verify）、impl **1103–1152**（每轮 1 片）、efforts **#1500–#1549**（R1 开张轮 = #1500，R2–R50 内容轮 = #1501–#1549；R51 收口轮不占新 effort 号）。fetch 时 github.com 443 不通（本地 main 领先 origin/main，本地即最新真相），实查本地全档：efforts 至 1400、specs 至 1400、tickets 至 T2102、impl 至 1053——**1500 系与 T2251+ 完全空闲**。
> **并存声明**：I（900 系）/L（1400 系）等并行会话 map 与本线互不触碰；每轮开工先 fetch 双查，push 被拒即 pull --rebase 后重推。
> 用户常设授权（沿 F/G/H/I/J/K/L 会话）：**全程 AFK，不问用户**——每轮 = wayfinder（决策票）→ to-spec → to-tickets → implement → git 自动提交推送 GitHub。

## Destination

**50 个完整自迭代 loop 全部完成**——每轮从高价值 GitHub 项目借鉴一个思想，落地为本仓一个小而完整、带测试、默认零行为变化或缺陷修复型/opt-in 的机制改进；每轮 Conventional Commits 提交并推送 GitHub；周期性（每 10 轮）+ 收口轮跑全仓 `mvn -B -ntp clean verify` 绿（16 模块 + 快照门 + SpecCoverage 覆盖门）。

## Notes

- 每轮固定四步产物：决策票（shape 票同轮开+解决，Resolution 注明「用户常设授权 AFK、可推翻」）→ `docs/spec/<15NN>-<slug>.md`（+README「生产级纵深 XI（M 会话 1500 系增量）」表行，SpecCoverageTest 门）→ impl 切片 → 模块代码+测试。
- 每轮模块级定向测试必须绿；新公共 api 面类型入轮再生 API 快照（优先嵌套 record 不进快照面）；每轮 commit 后 push，周期性合并 origin/main 防漂移。
- 选题纪律：每轮开工先缺口核查（grep spec+代码，**含 H 池 R1–R50+S1–S10 与 I/J/K/L 已落地主题一并回避**），已实现则台账记 `ruled-out` 顺延备选池；代码库 300+ effort 高度饱和，排重 grep 必须 `-i` 且按类名后缀查。
- 代码规范：无魔法数字（static final 常量或配置）、SLF4J/System.Logger 占位符、record/sealed 优先；进程级静态读面须配 reset 注入点与注释说明；测试无 Mockito——手写 fake/匿名类/lambda stub。
- 模块依赖边界不变：feature 模块互不直接依赖，跨机制协作走 core 事件总线或 core SPI。

## Decisions so far

（每轮 shape 票 Resolution 的 gist 逐轮补登于此）

- [SessionObserver 通知面异常隔离收口的形状裁决](../tickets/T2251-observer-notify-isolation-shape.md) — DefaultAgentSession 12 处观察者裸 forEach 通知点（onOpen/onTurnStart×2/onTurnEnd×3/onTurnError×5/onCancel）统一改走 notifyObservers 隔离派发：单个观察者 RuntimeException 记 ERROR 日志后继续其余观察者、不向上传播（onOpen 在构造器尾部未隔离时观测组件缺陷可炸掉整个会话构造且半初始化泄漏）——Guava EventBus SubscriberExceptionHandler 思想；impl-30 的 onClose/deliverEvent 隔离先例在 observer 其余回调面的补全；onClose 既有失败收集聚合语义不动。

## Not yet specified

- R2–R50 逐轮开工时按缺口核查选题。

## 轮次台账

| 轮 | 主题 | 借鉴源 | 票 | impl | spec | ✅ |
|---|------|--------|----|------|------|---|
| 1 | 开张轮：SessionObserver 通知面异常隔离（12 处裸 forEach → notifyObservers 隔离派发） | Guava EventBus SubscriberExceptionHandler | T2251–T2252 | 1103 | 1500 | ✅ |


## Out of scope

- 与 I/L 并行会话号段（900–999/1400–1449）重叠的任何占用。
