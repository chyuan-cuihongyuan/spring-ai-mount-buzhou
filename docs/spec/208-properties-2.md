# Spec 208 — 性质测试 II（effort #148）

> wayfinder map：`.wayfinder148/MAP.md`（T572–T573）。纯测试轮，spec 180 fog
> 收口。

## Solution

随机输入 × 不变量（259 例，种子 7）：①任意注入违规（大写/尾连字符/穿越/
超长）的租户 id 必被 forTenant 拒绝；②随机合法 id 的沙箱根必在 tenants/<t>
下且解析不越界；③虚拟键守恒——恰好用满后任意正额尝试恒 false 且耗尽粘性，
reset 后恢复（不凭空）。

## Testing Decisions

- 本轮即测试：259 例 + 租户/虚拟键例证回归全绿。
