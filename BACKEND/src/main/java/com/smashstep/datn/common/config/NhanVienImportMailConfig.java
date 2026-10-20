package com.smashstep.datn.common.config;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
@Configuration
public class NhanVienImportMailConfig {
    @Bean(name="nhanVienImportMailExecutor") public ThreadPoolTaskExecutor nhanVienImportMailExecutor(){
        var executor=new ThreadPoolTaskExecutor();executor.setCorePoolSize(1);executor.setMaxPoolSize(2);executor.setQueueCapacity(20);executor.setThreadNamePrefix("employee-import-mail-");executor.setWaitForTasksToCompleteOnShutdown(true);executor.setAwaitTerminationSeconds(20);return executor;
    }
}
