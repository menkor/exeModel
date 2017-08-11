package org.exemodel.orm.app;

import org.exemodel.orm.Statement;

/**
 * Created by zp on 17/6/19.
 */
public class CStatement extends Statement<CStatement>{

    public static CStatement build(Class modelClass){
        return new CStatement(modelClass);
    }

    public CStatement(Class<?> clazz) {
        super(clazz);
    }
}
