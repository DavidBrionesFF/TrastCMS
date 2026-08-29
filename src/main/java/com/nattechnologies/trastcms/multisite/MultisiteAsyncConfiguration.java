package com.nattechnologies.trastcms.multisite;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Keeps the resolved tenant when work crosses a thread boundary.
 *
 * <p>The executor is deliberately exposed using both conventional Spring names:
 * {@code taskExecutor} for {@code @Async} and {@code applicationTaskExecutor}
 * for Spring MVC/JPA integrations.</p>
 */
@Configuration(proxyBeanMethods = false)
public class MultisiteAsyncConfiguration {

    @Bean
    TaskDecorator siteContextTaskDecorator() {
        return task -> {
            SiteContext.ResolvedSite captured = SiteContext.current().orElse(null);
            return () -> {
                SiteContext.ResolvedSite previous = SiteContext.current().orElse(null);
                try {
                    if (captured == null) SiteContext.clear();
                    else SiteContext.set(captured);
                    task.run();
                } finally {
                    if (previous == null) SiteContext.clear();
                    else SiteContext.set(previous);
                }
            };
        };
    }

    @Bean(name = {"applicationTaskExecutor", "taskExecutor"})
    ThreadPoolTaskExecutor multisiteTaskExecutor(TaskDecorator siteContextTaskDecorator) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("trastcms-async-");
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(500);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setTaskDecorator(siteContextTaskDecorator);
        return executor;
    }
}
