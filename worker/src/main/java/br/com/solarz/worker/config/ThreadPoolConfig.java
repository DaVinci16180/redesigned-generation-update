package br.com.solarz.worker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class ThreadPoolConfig {
    public static final int numParallelThreads = 50;

    @Bean(name = "generationUpdate")
    public Executor atualizacaoGeracaoUmPorUmThreadPool() {
        var tpte = new ThreadPoolTaskExecutor();
        tpte.setCorePoolSize(numParallelThreads);
        tpte.setMaxPoolSize(numParallelThreads);
        tpte.setThreadNamePrefix("threadPool-");
        tpte.setBeanName("threadPool-");
        tpte.setKeepAliveSeconds(0);
        return tpte;
    }
}