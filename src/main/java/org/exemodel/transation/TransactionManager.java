package org.exemodel.transation;


import org.exemodel.session.AbstractSession;
import org.exemodel.session.Session;
import org.exemodel.session.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

public class TransactionManager extends AbstractPlatformTransactionManager {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionManager.class);

    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory){
        this.sessionFactory = sessionFactory;
    }

    @Override
    protected Object doGetTransaction() throws TransactionException {
        Session session = sessionFactory.currentSession();
        AbstractSession.setDefaultSessionFactory(sessionFactory);
        return session;
    }

    @Override
    protected void doBegin(Object o, TransactionDefinition transactionDefinition) throws TransactionException {
        sessionFactory.currentSession().begin();
    }

    @Override
    protected void doCommit(DefaultTransactionStatus defaultTransactionStatus) throws TransactionException {
//        LOG.info("db session commit");
        sessionFactory.currentSession().commit();
    }

    @Override
    protected void doRollback(DefaultTransactionStatus defaultTransactionStatus) throws TransactionException {
//        LOG.info("db session rollback");
        sessionFactory.currentSession().rollback();
    }

    @Override
    protected void doCleanupAfterCompletion(Object transaction) {
        Session session = (Session) transaction;
        if(session == null) {
            return;
        }
        session.close();

    }
}