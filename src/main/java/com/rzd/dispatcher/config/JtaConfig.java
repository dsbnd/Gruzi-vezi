package com.rzd.dispatcher.config;

import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

@Configuration
@Profile("wildfly")
public class JtaConfig {

    @Bean
    public PlatformTransactionManager transactionManager(
            UserTransaction userTransaction,
            TransactionManager transactionManager) {

        JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
        jtaTransactionManager.setUserTransaction(userTransaction);
        jtaTransactionManager.setTransactionManager(transactionManager);
        jtaTransactionManager.setAllowCustomIsolationLevels(true);

        return jtaTransactionManager;
    }
}