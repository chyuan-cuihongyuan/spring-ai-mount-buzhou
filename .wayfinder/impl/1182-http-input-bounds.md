# 1182 — http_request 输入边界四护栏

**What to build:** body/URL/头数量/单头值四护栏 + 专项异常分支。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 四常量护栏 + call() 前段检查 + HeaderTooLargeException 分支
- [x] 五断言 + tools 118 用例零回归

## Done

验证：`mvn -pl buzhou-tools test` 全绿。
