package org.exemodel.session.impl;

import org.exemodel.cache.ICache;
import org.exemodel.exceptions.JdbcRuntimeException;
import org.exemodel.session.SessionFactory;
import org.exemodel.session.AbstractSession;
import org.exemodel.session.Session;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author zp [15951818230@163.com]
 */
public class JdbcSessionFactory extends SessionFactory {

    private DataSource dataSource;
    private ICache cache;

    public JdbcSessionFactory(final DataSource dataSource, final ICache cache) {
        this.dataSource = dataSource;
        this.cache = cache;
        AbstractSession.setDefaultSessionFactoryIfEmpty(this);
    }

    @Override
    public Session createSession() {
        return new JdbcSession(this);
    }

    @Override
    public void close() throws SQLException{
        dataSource.getConnection().close();
    }

    @Override
    public Connection createJdbcConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new JdbcRuntimeException(e);
        }
    }


    @Override
    public ICache getCache() {
        return cache;
    }
}
