package org.exemodel.transation;

import org.exemodel.session.impl.JdbcSession;
import org.exemodel.exceptions.JdbcRuntimeException;

public class JdbcTransaction implements Transaction {
    private final JdbcSession jdbcSession;


    public JdbcTransaction(JdbcSession jdbcSession) {
        this.jdbcSession = jdbcSession;
    }

    @Override
    public void begin() {
        jdbcSession.setAutoCommit(false);
        jdbcSession.getActiveFlag().set(true);
    }

    @Override
    public void commit() {
        try {
            jdbcSession.getActiveFlag().set(true);
            jdbcSession.getJdbcConnection().commit();
            jdbcSession.getActiveFlag().set(false);
            jdbcSession.setAutoCommit(true);
        } catch (Exception e) {
            throw new JdbcRuntimeException(e);
        }
    }

    @Override
    public void rollback() {
        if (jdbcSession.getActiveFlag().get()) {
            return;
        }
        try {
            jdbcSession.getActiveFlag().set(false);
            jdbcSession.getJdbcConnection().rollback();
        } catch (Exception e) {
            throw new JdbcRuntimeException(e);
        }
    }

    @Override
    public boolean isActive() {
        return jdbcSession.getActiveFlag().get();
    }
}
