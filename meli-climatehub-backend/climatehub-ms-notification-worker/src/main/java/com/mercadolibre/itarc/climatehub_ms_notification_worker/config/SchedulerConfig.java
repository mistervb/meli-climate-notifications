package com.mercadolibre.itarc.climatehub_ms_notification_worker.config;

import com.mercadolibre.itarc.climatehub_ms_notification_worker.job.NotificationProcessorJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchedulerConfig {

    @Bean
    public JobDetail notificationProcessorJobDetail() {
        return JobBuilder.newJob(NotificationProcessorJob.class)
                .withIdentity("notificationProcessorJob", "notification-group")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger notificationProcessorTrigger(JobDetail notificationProcessorJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(notificationProcessorJobDetail)
                .withIdentity("notificationProcessorTrigger", "notification-group")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(1)
                        .repeatForever())
                .build();
    }
} 