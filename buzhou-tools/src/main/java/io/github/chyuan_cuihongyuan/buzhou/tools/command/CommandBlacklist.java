package io.github.chyuan_cuihongyuan.buzhou.tools.command;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 命令黑名单（spec 06 推演 #9 默认条目集）：通配匹配（{@code *} 任意串），大小写不敏感，
 * 命中即拒。默认条目：{@code rm -rf /}、{@code mkfs*}、{@code dd} 块设备写、
 * {@code shutdown}/{@code reboot}/{@code halt}、fork 炸弹模式。
 *
 * <p>黑名单天然防不全（spec 06 开放问题：白名单模式待细化）；本实现是默认安全兜底，
 * 经 {@code buzhou.tools.run-command.blacklist} 可整体替换。
 */
public class CommandBlacklist {

    /** 默认黑名单（通配模式，匹配前先做空白归一化）。 */
    public static final List<String> DEFAULT_PATTERNS = List.of(
            "rm -rf /",
            "rm -rf /*",
            "rm -fr /",
            "rm -fr /*",
            "mkfs*",
            "dd *of=/dev/*",
            "shutdown*",
            "reboot*",
            "halt*",
            ":(){:|:&};:",
            ":(){ :|:& };:",
            "*chmod -r 777 /*",
            "*mv /* /dev/null*",
            ">* /dev/sd*",
            "*dd if=/dev/zero of=/*");

    private final List<Pattern> patterns;

    public CommandBlacklist(List<String> wildcardPatterns) {
        this.patterns = (wildcardPatterns == null ? List.<String>of() : wildcardPatterns)
                .stream().map(CommandBlacklist::toPattern).toList();
    }

    public static CommandBlacklist defaults() {
        return new CommandBlacklist(DEFAULT_PATTERNS);
    }

    /** 命中黑名单返回 true；命令先折叠连续空白再匹配。 */
    public boolean matches(String command) {
        if (command == null) {
            return false;
        }
        String normalized = command.trim().replaceAll("\\s+", " ");
        return patterns.stream().anyMatch(p -> p.matcher(normalized).matches());
    }

    /** 通配转正则：{@code *} → {@code .*}，其余字符转义；整体匹配。 */
    private static Pattern toPattern(String wildcard) {
        StringBuilder regex = new StringBuilder();
        for (char c : wildcard.trim().replaceAll("\\s+", " ").toCharArray()) {
            if (c == '*') {
                regex.append(".*");
            } else {
                regex.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    }
}
