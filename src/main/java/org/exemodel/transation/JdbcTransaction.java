package org.exemodel.transation;

import org.exemodel.session.impl.JdbcSession;
import org.exemodel.exceptions.JdbcRuntimeException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author zp [15951818230@163.com]
 */
public class JdbcTransaction implements Transaction {
    private final JdbcSession jdbcSession;
    private Integer isolationLevel = null;


    public JdbcTransaction(JdbcSession jdbcSession) {
        this.jdbcSession = jdbcSession;
    }

    @Override
    public void begin() {
        jdbcSession.setAutoCommit(false);
        try {
            if(this.isolationLevel !=null){
                jdbcSession.getJdbcConnection().setTransactionIsolation(isolationLevel);
            }
        } catch (SQLException e) {
            throw new JdbcRuntimeException(e);
        }
    }

    @Override
    public void commit() {
        try {
            jdbcSession.getJdbcConnection().commit();
            jdbcSession.setAutoCommit(true);
            jdbcSession.close();
        } catch (Exception e) {
            throw new JdbcRuntimeException(e);
        }
    }

    @Override
    public void rollback() {
        try {
            jdbcSession.getActiveFlag().set(false);
            jdbcSession.getJdbcConnection().rollback();
            jdbcSession.close();
        } catch (Exception e) {
            throw new JdbcRuntimeException(e);
        }
    }

    public JdbcSession getJdbcSession() {
        return jdbcSession;
    }

    @Override
    public void close() {

    }

    @Override
    public Connection getConnection() {
        return null;
    }

    public Integer getIsolationLevel() {
        return isolationLevel;
    }

    @Override
    public void setIsolationLevel(Integer isolationLevel) {
        this.isolationLevel = isolationLevel;
    }
}
