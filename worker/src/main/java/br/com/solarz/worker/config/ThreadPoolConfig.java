package br.com.solarz.worker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class ThreadPoolConfig {
    public static final int numParallelThreads = 50;

    @Bean(name = "generationUpdate")
    public TaskExecutor taskExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(numParallelThreads);
        executor.setMaxPoolSize(numParallelThreads);
        executor.setThreadNamePrefix("generationUpdateThreadPool-");
        executor.setBeanName("generationUpdateThreadPool-");
        executor.setKeepAliveSeconds(0);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
    }
}