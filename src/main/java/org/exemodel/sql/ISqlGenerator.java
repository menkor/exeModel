package org.exemodel.sql;

import org.exemodel.orm.ModelMeta;

/**
 * Created by xiaofengxu on 18/6/4.
 */
public interface ISqlGenerator {

    String findById(ModelMeta modelMeta,boolean partition);

    String update(ModelMeta modelMeta);

    String insert(ModelMeta modelMeta);

    String delete(ModelMeta modelMeta);

}
