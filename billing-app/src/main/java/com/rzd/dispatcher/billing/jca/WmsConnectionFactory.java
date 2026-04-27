package com.rzd.dispatcher.billing.jca;

import jakarta.resource.cci.ConnectionFactory;
import jakarta.resource.ResourceException;

public interface WmsConnectionFactory extends ConnectionFactory {
    @Override
    WmsConnection getConnection() throws ResourceException;
}