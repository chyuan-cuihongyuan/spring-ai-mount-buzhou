# 1638 · 泄漏金丝雀 yml 装配（spec 1625 配置面补全）

> 来源：N 会话 R39（effort #1638 / T2427–T2428 / impl 1191）。

## Solution

`BuzhouGuardAutoConfiguration`：`buzhou.guard.leak-canary.salt` 非空即
`builder.leakCanary(salt)`（salt 建议从环境变量注入——canarytokens 的秘密
属性；代码库明文 salt 降低防护强度）。

## Testing Decisions

- `LeakCanaryAssemblyTest` 两断言：salt 装配产物（SessionCanaryHook 注册）
  种植 + 跨会话检出 + 自回显不算；无 salt 零 hook。
- 回归：guard 全量 370 用例。

## Out of Scope

- salt 的 jasypt/属性源加密（Spring 生态标准方案——文档指引即可）。
