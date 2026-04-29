package com.rzd.dispatcher.billing.jca;

import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ConnectionSpec;
import jakarta.resource.cci.RecordFactory;
import jakarta.resource.cci.ResourceAdapterMetaData;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import javax.naming.Reference;

@Component
public class LocalFileWmsConnectionFactory implements WmsConnectionFactory {

    @Override
    public WmsConnection getConnection() throws ResourceException {
        return new FileBasedWmsConnection();
    }

    @Override
    public Connection getConnection(ConnectionSpec connectionSpec) throws ResourceException {
        return null;
    }

    @Override
    public RecordFactory getRecordFactory() throws ResourceException {
        return null;
    }

    @Override
    public ResourceAdapterMetaData getMetaData() throws ResourceException {
        return null;
    }

    @Override
    public void setReference(Reference reference) {

    }

    @Override
    public Reference getReference() throws NamingException {
        return null;
    }
}