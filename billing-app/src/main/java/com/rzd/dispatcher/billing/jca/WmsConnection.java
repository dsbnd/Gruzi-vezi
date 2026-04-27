package com.rzd.dispatcher.billing.jca;

import jakarta.resource.cci.*;
import jakarta.resource.ResourceException;

public interface WmsConnection extends Connection, AutoCloseable {
    void sendShippingNote(String orderId, String xmlContent) throws ResourceException;

    @Override
    default Interaction createInteraction() throws ResourceException {
        return null;
    }

    @Override
    default LocalTransaction getLocalTransaction() throws ResourceException {
        return null;
    }

    @Override
    default ConnectionMetaData getMetaData() throws ResourceException {
        return null;
    }

    @Override
    default ResultSetInfo getResultSetInfo() throws ResourceException {
        return null;
    }

    @Override
    default void close() throws ResourceException {

    }
}