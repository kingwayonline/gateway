package com.lgak.filter;

import com.alibaba.fastjson.support.hsf.HSFJSONUtils;
import com.lgak.utils.StateEnum;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CheckLoginFilter {

    @Autowired
    private RedissonClient redissonClient;

    @Bean
    @Order(-1)
    public GlobalFilter checkLogin() {
        return (exchange, chain) -> {
            RequestPath path = exchange.getRequest().getPath();
            if ("/login".equals(path.toString())) {
                return chain.filter(exchange);
            }
            String token = exchange.getRequest().getHeaders().getFirst("token");
            String username = exchange.getRequest().getHeaders().getFirst("username");
            ServerHttpResponse response = exchange.getResponse();
            // 判断请求头token是否存在，如果不存在则返回错误信息，返回内容如下
            // {
            //     "status": "-1",
            //     "msg": "login error"
            // }
            if (StringUtils.isEmpty(token) || StringUtils.isEmpty(username)) {
                response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                byte[] bytes = (
                        "{\"status\":\"" + StateEnum.PARAMETER_ERROR.getState() + "\"," +
                                "\"msg\":\"" + StateEnum.PARAMETER_ERROR.getMsg() + "\"}").getBytes(StandardCharsets.UTF_8);
                DataBuffer buffer = response.bufferFactory().wrap(bytes);
                return exchange.getResponse().writeWith(Flux.just(buffer));
            }

            // 请求头token存在，验证有效性
            RMap<Object, Object> serverUser = redissonClient.getMap(username);
            if (!ObjectUtils.isEmpty(serverUser) && token.equals(serverUser.get("token"))) {
                return chain.filter(exchange);
            } else {
                response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                byte[] bytes = (
                        "{\"status\":\"" + StateEnum.LOGIN_TIME_OR_TOKEN_ERROR.getState() + "\"," +
                                "\"msg\":\"" + StateEnum.LOGIN_TIME_OR_TOKEN_ERROR.getMsg() + "\"}").getBytes(StandardCharsets.UTF_8);
                DataBuffer buffer = response.bufferFactory().wrap(bytes);
                return exchange.getResponse().writeWith(Flux.just(buffer));
            }

//            log.info("first pre filter");
//            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
//                log.info("third post filter");
//            }));
        };
    }

    @Bean
    @Order(0)
    public GlobalFilter setLoginTime() {
        return (exchange, chain) -> {
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                String username = exchange.getRequest().getHeaders().getFirst("username");
                RMap<Object, Object> map = redissonClient.getMap(username);
                map.expire(30, TimeUnit.MINUTES);
            }));
        };
    }

}
