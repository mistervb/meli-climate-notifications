package com.mercadolibre.itarc.climatehub_ms_notification_worker.service;

import com.mercadolibre.itarc.climatehub_ms_notification_worker.job.NotificationProcessorJob;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QuartzSchedulerService {

    private final Scheduler scheduler;

    public QuartzSchedulerService(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @PostConstruct
    public void init() {
        try {
            // Cria o job que vai processar as notificações
            JobDetail jobDetail = JobBuilder.newJob(NotificationProcessorJob.class)
                    .withIdentity("notificationProcessor", "notification")
                    .storeDurably()
                    .build();

            // Cria o trigger para executar a cada minuto
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("notificationTrigger", "notification")
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInMinutes(1)
                            .repeatForever())
                    .build();

            // Agenda o job
            scheduler.scheduleJob(jobDetail, trigger);
            
            log.info("🚀 Job de processamento de notificações agendado com sucesso");
            
        } catch (SchedulerException e) {
            log.error("❌ Erro ao agendar job de processamento de notificações", e);
        }
    }
} 