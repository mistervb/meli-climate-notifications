package com.mercadolibre.itarc.climatehub_ms_notification_worker.config;

import org.springframework.boot.autoconfigure.quartz.QuartzProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class QuartzConfig {

    @Bean
    public SpringBeanJobFactory springBeanJobFactory(ApplicationContext applicationContext) {
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        return jobFactory;
    }

    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer(
            DataSource dataSource,
            SpringBeanJobFactory jobFactory,
            QuartzProperties quartzProperties
    ) {
        return factory -> {
            factory.setDataSource(dataSource);
            factory.setJobFactory(jobFactory);
            
            Properties props = new Properties();
            props.putAll(quartzProperties.getProperties());
            factory.setQuartzProperties(props);
            
            factory.setWaitForJobsToCompleteOnShutdown(true);
            factory.setOverwriteExistingJobs(true);
            factory.setAutoStartup(true);
            factory.setSchedulerName("climatehub-notification-scheduler");
        };
    }
} 