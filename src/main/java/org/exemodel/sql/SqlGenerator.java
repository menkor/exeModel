package org.exemodel.sql;

import org.exemodel.orm.ModelMeta;

/**
 * Created by xiaofengxu on 18/6/4.
 */
public class SqlGenerator implements ISqlGenerator {
    @Override
    public String findById(ModelMeta modelMeta, boolean partition) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ");
        sql.append(modelMeta.getTableName());
        sql.append(" WHERE ");
        if (partition) {
            generatePartitionCondition(modelMeta,sql);
        }
        sql.append(modelMeta.getIdColumnMeta().columnName);
        sql.append("=? limit 1");
        return sql.toString();
    }

    @Override
    public String update(ModelMeta modelMeta) {
        boolean first = true;
        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(modelMeta.getTableName());
        sql.append(" SET ");
        for (ModelMeta.ModelColumnMeta columnMeta : modelMeta.getColumnMetaSet()) {
            if (!columnMeta.isId && !columnMeta.isPartition) {// id and partitionId can't set
                if (first) {
                    first = false;
                } else {
                    sql.append(",");
                }
                sql.append(columnMeta.columnName);
                sql.append("=? ");
            }
        }
        sql.append(" WHERE ");
        generatePartitionCondition(modelMeta,sql);
        sql.append(modelMeta.getIdColumnMeta().columnName);
        sql.append("=?");

        return sql.toString();
    }

    @Override
    public String insert(ModelMeta modelMeta) {
        boolean first = true;
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(modelMeta.getTableName());
        sql.append(" (");

        for (ModelMeta.ModelColumnMeta columnMeta : modelMeta.getColumnMetaSet()) {
            if (first) {
                first = false;
            } else {
                sql.append(",");
            }
            sql.append(columnMeta.columnName);
        }

        sql.append(")");
        sql.append(" VALUES (");
        int size = modelMeta.getColumnMetaSet().size();
        for (int i = 0; i < size; i++) {
            if (i != 0) {
                sql.append(",");
            }
            sql.append('?');
        }
        sql.append(")");
        return sql.toString();
    }

    @Override
    public String delete(ModelMeta modelMeta) {
        return null;
    }


    private void generatePartitionCondition(ModelMeta modelMeta,StringBuilder sql) {
        if (modelMeta.getPartitionColumn()!= null) {
            sql.append(modelMeta.getPartitionColumn().columnName);
            sql.append("=? and ");
        }
    }
}
