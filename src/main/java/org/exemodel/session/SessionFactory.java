package org.exemodel.session;

import org.exemodel.cache.ICache;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by zp on 2016/7/18
 */
public abstract class SessionFactory {

    protected final  ThreadLocal<Session> sessionThreadLocal = new ThreadLocal<Session>() {
        @Override
        public Session initialValue() {
            return createSession();
        }

        @Override
        public Session get() {
            Session session = super.get();
            if (session != null) {
                if (!session.isOpen()) {
                    this.remove();
                    session = initialValue();
                }
            }
            return session;
        }


    };

    public Session getThreadScopeSession() {
        return sessionThreadLocal.get();
    }

    public Session currentSession() {
        return getThreadScopeSession();
    }

    public abstract ICache getCache();

    public  abstract   Session createSession();

    public  abstract Connection createJdbcConnection() ;

    public  abstract void close() throws SQLException;
}
