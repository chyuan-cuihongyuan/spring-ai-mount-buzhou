package io.github.chyuan_cuihongyuan.buzhou.guard.secret;

import java.util.regex.Pattern;

/**
 * 密钥类型（spec 400 / T691，gitleaks 借鉴）：有界枚举——7 型凭据签名，
 * 每型自带正则（命名组不含——区间由 Matcher 给出）。基数纪律：计数器
 * tag type 以本枚举为界，勿加无界字符串面。
 */
public enum SecretType {

    /** AWS 访问键 ID（AKIA 开头 16 位大写字母数字）。 */
    AWS_ACCESS_KEY(Pattern.compile("(?<![A-Z0-9])AKIA[0-9A-Z]{16}(?![A-Z0-9])")),

    /** GitHub token（ghp_/gho_/ghu_/ghs_/ghr_ 前缀）。 */
    GITHUB_TOKEN(Pattern.compile("(?<![A-Za-z0-9])gh[pousr]_[A-Za-z0-9]{20,}(?![A-Za-z0-9])")),

    /** Google API key（AIza 开头 35 位）。 */
    GOOGLE_API_KEY(Pattern.compile("(?<![A-Za-z0-9_-])AIza[0-9A-Za-z_-]{35}(?![A-Za-z0-9_-])")),

    /** Slack token（xoxb-/xoxa-/xoxp-/xoxr-/xoxs- 前缀）。 */
    SLACK_TOKEN(Pattern.compile("(?<![A-Za-z0-9-])xox[baprs]-[0-9A-Za-z-]{10,}(?![A-Za-z0-9-])")),

    /** OpenAI 风格 key（sk- 前缀，长度下限 30 防误伤普通词）。 */
    OPENAI_STYLE_KEY(Pattern.compile("(?<![A-Za-z0-9_-])sk-[A-Za-z0-9_-]{30,}(?![A-Za-z0-9_-])")),

    /** JWT（三段 base64url，头段 eyJ 起）。 */
    JWT(Pattern.compile("eyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{5,}")),

    /** PEM 私钥块（BEGIN 到 END 整块含边界）。 */
    PRIVATE_KEY_BLOCK(Pattern.compile(
            "-----BEGIN (?:RSA |EC |DSA |OPENSSH |ENCRYPTED )?PRIVATE KEY-----"));

    private final Pattern pattern;

    SecretType(Pattern pattern) {
        this.pattern = pattern;
    }

    Pattern pattern() {
        return pattern;
    }
}
