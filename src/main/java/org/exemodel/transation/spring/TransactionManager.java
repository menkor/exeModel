package org.exemodel.transation.spring;

import org.exemodel.session.Session;
import org.exemodel.session.SessionFactory;
import org.exemodel.transation.Transaction;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

/**
 * @author zp [15951818230@163.com]
 */
public class TransactionManager extends AbstractPlatformTransactionManager {

    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory){
        this.sessionFactory = sessionFactory;
    }

    @Override
    protected Object doGetTransaction() throws TransactionException {
        return sessionFactory.currentSession().getTransaction();
    }

    @Override
    protected void doBegin(Object o, TransactionDefinition transactionDefinition) throws TransactionException {
        Transaction transaction = (Transaction) o;
        transaction.setIsolationLevel(transactionDefinition.getIsolationLevel());
        sessionFactory.currentSession().begin();

    }

    @Override
    protected void doCommit(DefaultTransactionStatus defaultTransactionStatus) throws TransactionException {
        sessionFactory.currentSession().commit();
    }

    @Override
    protected void doRollback(DefaultTransactionStatus defaultTransactionStatus) throws TransactionException {
        sessionFactory.currentSession().rollback();
    }

    @Override
    protected void doCleanupAfterCompletion(Object transaction) {
        Session session = sessionFactory.currentSession();
        session.close();
    }

    @Override
    protected boolean isExistingTransaction(Object transaction) throws TransactionException {
        return sessionFactory.currentSession().isRunning();
    }


}