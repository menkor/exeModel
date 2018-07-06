package org.exemodel.transation;

import java.sql.Connection;

/**
 * Created by zp on 2016/7/18
 */
public interface Transaction {

    void begin();

    void commit();

    void rollback();

    void close();

    Connection getConnection();

    public void setIsolationLevel(Integer isolationLevel);

}
