package io.github.chyuan_cuihongyuan.buzhou.guard.policy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * 策略规则 JSON 解析（impl-40 / spec 13 §T64）：热加载文件的可分析子集格式——
 *
 * <pre>
 * {"rules":[
 *   {"id":"allow-trusted-read","toolPattern":"read_*",
 *    "labels":[{"key":"taint","op":"eq","value":"TRUSTED"}],
 *    "action":"ALLOW","reason":"受信上下文读放行"},
 *   {"id":"deny-untrusted","toolPattern":"*",
 *    "labels":[{"key":"taint","op":"eq","value":"UNTRUSTED"}],
 *    "action":"DENY","reason":"非受信上下文一律拒"}
 * ]}
 * </pre>
 *
 * <p>解析失败抛 {@link IllegalArgumentException}（带定位）——由刷新器兜住沿用旧快照，
 * <b>绝不部分生效</b>（parse-all-or-nothing）。
 */
public final class PolicyRuleParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PolicyRuleParser() {
    }

    public static List<PolicyDecision.Rule> parse(String json) {
        JsonNode root;
        try {
            root = MAPPER.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("策略文件不是合法 JSON：" + e.getMessage(), e);
        }
        JsonNode rulesNode = root.get("rules");
        if (rulesNode == null || !rulesNode.isArray()) {
            throw new IllegalArgumentException("策略文件缺少 \"rules\" 数组");
        }
        List<PolicyDecision.Rule> rules = new ArrayList<>();
        for (int i = 0; i < rulesNode.size(); i++) {
            rules.add(parseRule(rulesNode.get(i), i));
        }
        return List.copyOf(rules);
    }

    private static PolicyDecision.Rule parseRule(JsonNode node, int index) {
        String id = textOf(node.get("id"), null);
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("规则[" + index + "] 缺少 id");
        }
        String actionText = textOf(node.get("action"), null);
        PolicyDecision.Action action;
        try {
            action = PolicyDecision.Action.valueOf(actionText == null ? "" : actionText);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("规则[" + index + "]（" + id
                    + "）action 非法：须 ALLOW/DENY/ESCALATE（收到 " + actionText + "）");
        }
        List<PolicyDecision.LabelPredicate> labels = new ArrayList<>();
        JsonNode labelsNode = node.get("labels");
        if (labelsNode != null && labelsNode.isArray()) {
            for (JsonNode labelNode : labelsNode) {
                String key = textOf(labelNode.get("key"), null);
                String op = textOf(labelNode.get("op"), "eq");
                if (key == null || key.isBlank()) {
                    throw new IllegalArgumentException("规则[" + id + "] label 缺少 key");
                }
                if (!"eq".equals(op) && !"exists".equals(op) && !"contains".equals(op)) {
                    throw new IllegalArgumentException("规则[" + id + "] label op 非法："
                            + op + "（须 eq/exists/contains）");
                }
                labels.add(new PolicyDecision.LabelPredicate(key, op,
                        textOf(labelNode.get("value"), null)));
            }
        }
        return new PolicyDecision.Rule(id, textOf(node.get("toolPattern"), "*"), labels,
                new PolicyDecision(action, textOf(node.get("reason"), "规则 " + id)));
    }

    private static String textOf(JsonNode node, String defaultValue) {
        return node == null || node.isNull() ? defaultValue : node.asText();
    }
}
