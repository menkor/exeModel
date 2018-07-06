package org.exemodel.builder;

import org.exemodel.orm.ModelMeta;

/**
 * @author zp [15951818230@163.com]
 */
public class CrudSqlGenerator {
    private ModelMeta modelMeta;
    private String findByIdSql;
    private String findByPartitionIdSql;
    private String deleteSql;
    private String insertSql;
    private String updateSql;

    public CrudSqlGenerator(ModelMeta modelMeta) {
        this.modelMeta = modelMeta;
    }

    public String getFindByIdSql() {
        if (findByIdSql == null) {
            findByIdSql = findById(modelMeta, false);
        }
        return findByIdSql;
    }

    public String getFindByPartitionIdSql() {
        if (findByPartitionIdSql == null) {
            findByPartitionIdSql = findById(modelMeta, true);
        }
        return findByPartitionIdSql;
    }

    public String getUpdateSql() {
        if (updateSql == null) {
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
            generatePartitionCondition(modelMeta, sql);
            sql.append(modelMeta.getIdColumnMeta().columnName);
            sql.append("=?");
            return updateSql = sql.toString();
        }
        return updateSql;
    }


    public String getInsertSql() {
        if (insertSql == null) {
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
            return insertSql = sql.toString();
        }
        return insertSql;
    }


    public  String getDeleteSql() {
        if(deleteSql ==null){
            StringBuilder sql = new StringBuilder("DELETE FROM ");
            sql.append(modelMeta.getTableName());
            sql.append(" WHERE ");
            generatePartitionCondition(modelMeta, sql);
            sql.append(modelMeta.getIdColumnMeta().columnName);
            sql.append("=?");
            return deleteSql = sql.toString();
        }
        return deleteSql;
    }


    private static String findById(ModelMeta modelMeta, boolean partition) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ");
        sql.append(modelMeta.getTableName());
        sql.append(" WHERE ");
        if (partition) {
            generatePartitionCondition(modelMeta, sql);
        }
        sql.append(modelMeta.getIdColumnMeta().columnName);
        sql.append("=? limit 1");
        return sql.toString();
    }

    private static void generatePartitionCondition(ModelMeta modelMeta, StringBuilder sql) {
        if (modelMeta.getPartitionColumn() != null) {
            sql.append(modelMeta.getPartitionColumn().columnName);
            sql.append("=? and ");
        }
    }


}
