# 1520 — 构造器 this 逃逸修复 + 三裁定（六-7/五-2/六-2 指纹）

> 来源：M 会话第 23 轮 = effort #1520（impl 1123）。

## 裁定与修复

- 六-7：AsyncObservabilityPipeline 构造器立即 start 改惰性（首事件 CAS 启动，零事件零线程；close isAlive 防御）；DbToolSetProvider 不整改（首跑延迟 pollInterval，窗口理论性）；
- 五-2：CLAUDE 规约追认边界——三键内 opt-in 装配允许 getProperty 直读（10 处先例），复杂面必须 record；
- 六-2 指纹：双轨不统一（指纹值稳定性 > 材质统一——统一即部署授权失效），交叉注记。

## 兼容性

惰性启动行为等价（首事件前无 drain 需求）；其余纯文档。
