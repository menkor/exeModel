package org.exemodel.transation;

/**
 * Created by zp on 2016/7/18
 */
public interface Transaction {

    public void begin();

    public void commit();

    public void rollback();

    public boolean isActive();
}
