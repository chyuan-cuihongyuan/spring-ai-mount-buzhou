package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.context.ApplicationEvent;

/**
 * 配置刷新事件（spec 320 / T631，Spring Cloud Context EnvironmentChangeEvent
 * 思想——但不引 spring-cloud 依赖）：宿主改完 PropertySource（ConfigMap watch /
 * 管理端点 / 自有通道）后发布；空标记无载荷——各容量面监听器自重读
 * {@link org.springframework.core.env.Environment}。事件只做「该重读了」信号。
 */
public class BuzhouConfigRefreshEvent extends ApplicationEvent {

    public BuzhouConfigRefreshEvent(Object source) {
        super(source);
    }
}
