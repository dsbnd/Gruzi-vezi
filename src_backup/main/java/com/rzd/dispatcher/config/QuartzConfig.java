package com.rzd.dispatcher.config;

import com.rzd.dispatcher.job.AutoDeliveryJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    // 1. Описываем саму задачу (указываем класс)
    @Bean
    public JobDetail autoDeliveryJobDetail() {
        return JobBuilder.newJob(AutoDeliveryJob.class)
                .withIdentity("autoDeliveryJob")
                .storeDurably()
                .build();
    }

    // 2. Настраиваем триггер (расписание)
    @Bean
    public Trigger autoDeliveryJobTrigger(JobDetail autoDeliveryJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(autoDeliveryJobDetail)
                .withIdentity("autoDeliveryTrigger")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(1) // Запуск каждую 1 минуту
                        .repeatForever())
                .build();
    }
}