package io.github.chyuan_cuihongyuan.buzhou.resilience.routing;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.WeightedRouter;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 多模型加权路由（spec 339 / T669，LiteLLM Router / OpenRouter 借鉴——
 * 199 平滑加权原语的消费兑现）：逐 call/stream 经 Nginx smooth WRR 选
 * 一路模型——比例精确、时间平滑（5:1:1 不连五爆发）、同序可复现。
 *
 * <p>Prompt 原样透传（模型特定 options 兼容归宿主——137 对冲同口径）；
 * stream 与 call 各自独立 pick（不混流、不做同 prompt 粘性）。计数
 * {@code buzhou.routing.routed}（tag model=beanName）。装饰器零侵入——
 * 宿主把它当主模型挂进任何装配位。
 */
public final class WeightedChatModel implements ChatModel {

    private final Map<String, ChatModel> candidates;
    private final WeightedRouter<ChatModel> router;

    /**
     * @param candidates 有序候选：beanName → ChatModel（权重在 weights，同键）
     * @param weights    beanName → 正整数权重
     */
    public WeightedChatModel(Map<String, ChatModel> candidates, Map<String, Integer> weights) {
        if (candidates == null || candidates.size() < 2) {
            throw new IllegalArgumentException("candidates 至少两路——单路直用即可无需路由");
        }
        this.candidates = new LinkedHashMap<>(candidates);
        WeightedRouter.Pair<ChatModel>[] pairs = candidates.keySet().stream()
                .map(name -> WeightedRouter.Pair.of(candidates.get(name),
                        weightOf(weights, name)))
                .toArray(WeightedRouter.Pair[]::new);
        this.router = WeightedRouter.of(pairs);
    }

    private static int weightOf(Map<String, Integer> weights, String name) {
        Integer weight = weights == null ? null : weights.get(name);
        if (weight == null || weight < 1) {
            throw new IllegalArgumentException(
                    "路由权重非法（" + name + " → " + weight + "）——每路正整数 ≥1");
        }
        return weight;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        ChatModel route = route();
        return route.call(prompt);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        ChatModel route = route();
        return route.stream(prompt);
    }

    private ChatModel route() {
        ChatModel picked = router.pick()
                .orElseThrow(() -> new IllegalStateException("路由器空——构造期已挡"));
        BuzhouMetricsHolder.metrics().counter("buzhou.routing.routed", 1,
                "model", nameOf(picked));
        return picked;
    }

    private String nameOf(ChatModel model) {
        return candidates.entrySet().stream()
                .filter(e -> e.getValue() == model)
                .map(Map.Entry::getKey)
                .findFirst().orElse("unknown");
    }

    /** 路由观测面（beanName → 权重）。 */
    public Map<String, Integer> routes() {
        Map<String, Integer> view = new LinkedHashMap<>();
        candidates.forEach((name, model) ->
                view.put(name, router.weights().getOrDefault(model, 0)));
        return view;
    }
}
