# Spec 10000 — X 系 X1 对账门落位（effort #10000，X1）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10001–X10002，impl 2403）。
> 借鉴：WSession9000LedgerAuditTest 公式族第十一应用——「纪律变测试」的预防式对账

## Problem Statement

新会话开图即须落对账门：五位数号段（10000 系）
工件链四面互证——spec↔README↔票↔impl，
号段公式声明先行，缺位即红。

## Solution

XSession10000LedgerAuditTest（starter 测试包）：
spec N → shape 票 10001+2(N−10000)/verify=+1/
impl 2403+(N−10000)；5 位数字前缀文件名适配；
范围自扩展（扫现有 spec 驱动）；README 覆盖
断言（10000 系在 SpecCoverageTest \d{1,4} 之外
——由本门接管）。

## Testing Decisions

X1 落位即验：spec 10000 四测全绿（票对/impl/
README/严格递增）；后续轮落地自动纳入。

## Out of Scope

不做全仓 verify（R48/V48/W 环境豁免口径延续）；
不改 SpecCoverageTest 正则（老号段口径不动）。

## Further Notes

X 会话 10000 系 50 轮开图轮（A–W 字母已占用
接续）；选题静脉 8 波 40 组件预排入总图；
GitHub 离线口径入档。
