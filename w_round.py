#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""W 会话轮工件五合一：spec / shape+verify 票对 / impl 切片 / README 行 / map 翻转。
用法：python w_round.py <N> <slug> <readme_cat> <readme_name> <readme_desc> <verify_cmd> <borrow> <shape_res> <verify_res>
  N            轮号（2..50）；spec=N+8998；shape 票=9001+2(N-1)；impl=2352+N
  slug         spec/impl 文件名短横线 slug（如 hopcroft-karp）
  readme_cat   README 表「领域」列
  readme_name  README 表「能力」列
  readme_desc  README 表「说明」列（含 (spec NNNN) 尾注，由本脚本追加）
  verify_cmd   impl 切片里的验证命令（不含反引号）
  borrow       spec「借鉴」行
  shape_res    shape 票 Resolution 文本（单行）
  verify_res   verify 票 Resolution 文本（单行）
其余 spec 段（problem/solution/testing/out/notes）由组件轮内联编辑或走本脚本默认。
"""
import io
import sys


def w(path, text):
    with io.open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(text)


def main():
    (n, slug, cat, name, desc, verify_cmd, borrow, shape_res, verify_res) = sys.argv[1:10]
    n = int(n)
    spec = 8999 + n
    shape = 9001 + 2 * (n - 1)
    impl = 2352 + n
    day = '2026-09-30'

    spec_md = (
        '# Spec {sp} — {nm}（effort #{ef}，W{n}）\n\n'
        '> wayfinder map：`.wayfinder/maps/effort-9000.md`（W{sh}–W{sv}，impl {im}）。\n'
        '> 借鉴：{bo}\n\n'
        '## Problem Statement\n\n{PROBLEM}\n\n'
        '## Solution\n\n{SOLUTION}\n\n'
        '## Testing Decisions\n\n{TESTING}\n\n'
        '## Out of Scope\n\n{OUT}\n\n'
        '## Further Notes\n\n{NOTES}\n'
    ).format(sp=spec, nm=name, ef=spec, n=n, sh=shape, sv=shape + 1, im=impl, bo=borrow,
             PROBLEM='__PROBLEM__', SOLUTION='__SOLUTION__', TESTING='__TESTING__',
             OUT='__OUT__', NOTES='__NOTES__')
    w('docs/spec/%d-%s.md' % (spec, slug), spec_md)

    shape_md = (
        '---\nid: W{sh}\ntitle: W 会话 W{n} {nm} 的形状裁决\ntype: task\nstatus: closed\n'
        'assignee: zcode-w\nblocked-by: []\ncreated: {d}\n---\n\n'
        '## Question\n\n形状怎么定？\n\n## Resolution\n\n{res}\n'
    ).format(sh=shape, n=n, nm=name, d=day, res=shape_res)
    w('.wayfinder/tickets/W%d-%s-shape.md' % (shape, slug), shape_md)

    verify_md = (
        '---\nid: W{sv}\ntitle: W 会话 W{n} {nm} 的验证裁决\ntype: task\nstatus: closed\n'
        'assignee: zcode-w\nblocked-by: [W{sh}]\ncreated: {d}\n---\n\n'
        '## Question\n\nW{n} 合同怎么逐一验绿？\n\n## Resolution\n\n{res}\n'
    ).format(sv=shape + 1, n=n, nm=name, sh=shape, d=day, res=verify_res)
    w('.wayfinder/tickets/W%d-%s-verify.md' % (shape + 1, slug), verify_md)

    impl_md = (
        '# impl {im} — W 会话 W{n} {nm}（spec {sp} / W{sh}–W{sv} / W{n}）\n\n'
        '纵切片：{nm}——{ds}\n\n'
        '- 验证：`{vc}` 全绿。\n'
    ).format(im=impl, n=n, nm=name, sp=spec, sh=shape, sv=shape + 1, ds=shape_res, vc=verify_cmd)
    w('.wayfinder/impl/%d-%s.md' % (impl, slug), impl_md)

    # README 行：锚在 W 会话最近一行（spec 锚链——逐轮追加在 W 系前一行之后）
    p = 'README.md'
    s = io.open(p, encoding='utf-8').read()
    row = '| %s | %s | %s（spec %d） | [spec %d](docs/spec/%d-%s.md) |\n' % (
        cat, name, desc, spec, spec, spec, slug)
    anchor = 'docs/spec/%d-%s.md) |\n' % (spec - 1, '__ANCHOR__')
    # 直接找上一轮 spec 链接行（W 系严格连续）
    import re
    prev_spec = spec - 1
    m = re.search(r'\[spec %d\]\(docs/spec/%d-[\w.-]+\.md\) \|\n' % (prev_spec, prev_spec), s)
    if m:
        s = s[:m.end()] + row + s[m.end():]
    else:
        # W1 锚
        a1 = 'docs/spec/9000-w-w1-gate.md) |\n'
        assert a1 in s, 'README 锚缺失'
        s = s.replace(a1, a1 + row, 1)
    w(p, s)

    # map 翻转
    p = '.wayfinder/maps/effort-9000.md'
    s = io.open(p, encoding='utf-8').read()
    old = '| W%d | #9000 |' % n
    lines = s.split('\n')
    for i, ln in enumerate(lines):
        if ln.startswith(old):
            lines[i] = ln.replace('| ⬜ |', '| ✅ |')
    w(p, '\n'.join(lines))
    print('round W%d artifacts done: spec %d tickets W%d/W%d impl %d' % (n, spec, shape, shape + 1, impl))


if __name__ == '__main__':
    main()
