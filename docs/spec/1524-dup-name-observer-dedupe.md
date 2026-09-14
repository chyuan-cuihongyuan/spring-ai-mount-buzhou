# 1524 — 配置错误显形双小项（hook 重名 / observer 重复注册）

> 来源：M 会话第 27 轮 = effort #1524（impl 1127）。

## 目标

- HookChain 构造期重复 hook 名 WARN（派发序不稳定/对位歧义信号，不炸装配）；
- SessionAssemblyContext.addObserver 同实例幂等去重（双份通知是装配错误；listener 域维持 List.add——lambda 多实例 identity 去重无意义）。

## 兼容性

重复注册行为从双计变单计（缺陷修复面）；正常装配零变化。
