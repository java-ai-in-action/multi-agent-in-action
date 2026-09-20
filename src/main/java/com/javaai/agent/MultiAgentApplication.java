package com.javaai.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 企业级 Multi-Agent 客服系统示例入口。
 *
 * <p>配套文章：篇6《我用 Spring AI Alibaba + Nacos 搭了个企业级 Multi-Agent，老板看完沉默了》
 */
@SpringBootApplication
@EnableDiscoveryClient
public class MultiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultiAgentApplication.class, args);
    }
}
