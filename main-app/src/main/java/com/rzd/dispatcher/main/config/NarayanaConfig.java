package com.rzd.dispatcher.main.config;

import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.ats.jta.common.jtaPropertyManager;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.transaction.jta.JtaTransactionManager;

@Configuration
public class NarayanaConfig {

    @Bean
    public UserTransaction userTransaction() {
        JTAEnvironmentBean jtaEnvironmentBean = jtaPropertyManager.getJTAEnvironmentBean();
        return jtaEnvironmentBean.getUserTransaction();
    }

    @Bean
    public TransactionManager narayanaTransactionManager() {  
        JTAEnvironmentBean jtaEnvironmentBean = jtaPropertyManager.getJTAEnvironmentBean();
        return jtaEnvironmentBean.getTransactionManager();
    }

    @Bean(name = "transactionManager")  
    @DependsOn({"userTransaction", "narayanaTransactionManager"})
    public JtaTransactionManager jtaTransactionManager(
            UserTransaction userTransaction,
            TransactionManager narayanaTransactionManager) {  

        JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
        jtaTransactionManager.setUserTransaction(userTransaction);
        jtaTransactionManager.setTransactionManager(narayanaTransactionManager);
        jtaTransactionManager.setAllowCustomIsolationLevels(true);

        return jtaTransactionManager;
    }
}