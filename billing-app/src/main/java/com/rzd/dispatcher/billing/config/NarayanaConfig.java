package com.rzd.dispatcher.billing.config;

import com.arjuna.ats.jta.common.jtaPropertyManager;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

@Configuration
public class NarayanaConfig {

    @Bean
    public UserTransaction userTransaction() {
        return com.arjuna.ats.jta.UserTransaction.userTransaction();
    }

    @Bean
    public TransactionManager narayanaTransactionManager() {
        return jtaPropertyManager.getJTAEnvironmentBean().getTransactionManager();
    }

    @Bean
    public PlatformTransactionManager transactionManager(
            UserTransaction ut, TransactionManager tm) {
        JtaTransactionManager jtaTm = new JtaTransactionManager();
        jtaTm.setUserTransaction(ut);
        jtaTm.setTransactionManager(tm);
        jtaTm.setAllowCustomIsolationLevels(true);
        return jtaTm;
    }
}