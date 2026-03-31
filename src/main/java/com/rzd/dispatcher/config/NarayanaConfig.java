package com.rzd.dispatcher.config;

import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.ats.jta.common.jtaPropertyManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.jta.JtaTransactionManager;

import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

@Configuration
public class NarayanaConfig {

    @Bean
    public JtaTransactionManager jtaTransactionManager(
            UserTransaction userTransaction,
            TransactionManager transactionManager) {

        JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
        jtaTransactionManager.setUserTransaction(userTransaction);
        jtaTransactionManager.setTransactionManager(transactionManager);
        jtaTransactionManager.setAllowCustomIsolationLevels(true);

        return jtaTransactionManager;
    }

    @Bean
    public UserTransaction userTransaction() {
        JTAEnvironmentBean jtaEnvironmentBean = jtaPropertyManager.getJTAEnvironmentBean();
        return jtaEnvironmentBean.getUserTransaction();
    }

    @Bean
    public TransactionManager transactionManager() {
        JTAEnvironmentBean jtaEnvironmentBean = jtaPropertyManager.getJTAEnvironmentBean();
        return jtaEnvironmentBean.getTransactionManager();
    }
}