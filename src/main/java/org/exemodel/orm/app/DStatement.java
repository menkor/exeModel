package org.exemodel.orm.app;

import org.exemodel.orm.Statement;
import org.exemodel.session.Session;

/**
 * Created by zp on 17/6/19.
 *  for drds application
 */
public class DStatement extends Statement<DStatement> {

    public static DStatement build(Class modelClass){
        return new DStatement(modelClass);
    }

    public DStatement(Class<?> clazz) {
        super(clazz);
    }

    public DStatement partitionId(Object value){
        return eq(getModelMeta().getPartitionColumn().columnName,value);
    }

    /**
     * get auto_increment_id ,when DRDS it get Sequence
     *
     * @return
     */
    public long generateId() {
        return (Long) getSession().findOneByNativeSql(Long.class,
                "select AUTO_SEQ_"+getModelMeta().getTableName()+".NEXTVAL");
    }
}
