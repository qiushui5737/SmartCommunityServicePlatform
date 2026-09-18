package com.community.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置
 *
 * 用途：账单生成后异步推送站内通知、日志异步写入等
 *
 * 拒绝策略：CallerRunsPolicy — 队列满时由调用线程执行，防止任务丢失
 */
@Slf4j
@EnableAsync
@Configuration
public class AsyncConfig {

    @Value("${thread-pool.core-pool-size:4}")
    private int corePoolSize;

    @Value("${thread-pool.max-pool-size:8}")
    private int maxPoolSize;

    @Value("${thread-pool.queue-capacity:100}")
    private int queueCapacity;

    @Value("${thread-pool.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    @Bean("asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("async-task-");

        // 队列满时由调用线程执行，不丢弃任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 优雅关闭：等待任务完成再关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        log.info("异步线程池初始化完成 core={} max={} queue={}",
                corePoolSize, maxPoolSize, queueCapacity);
        return executor;
    }
}
