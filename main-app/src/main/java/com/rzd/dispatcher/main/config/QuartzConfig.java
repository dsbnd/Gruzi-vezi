package com.rzd.dispatcher.main.config;


import com.rzd.dispatcher.main.job.AutoDeliveryJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail autoDeliveryJobDetail() {
        return JobBuilder.newJob(AutoDeliveryJob.class)
                .withIdentity("autoDeliveryJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger autoDeliveryJobTrigger(JobDetail autoDeliveryJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(autoDeliveryJobDetail)
                .withIdentity("autoDeliveryTrigger")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(10)
                        .repeatForever())
                .build();
    }
}