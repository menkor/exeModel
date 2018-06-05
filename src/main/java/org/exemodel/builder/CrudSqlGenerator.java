package org.exemodel.builder;

import org.exemodel.orm.ModelMeta;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zp on 18/6/5.
 */
public class CrudSqlGenerator {

    private final static Map<Class, String> findByIdSqlCache = new ConcurrentHashMap<>();
    public String getFindByIdSql(final Class clazz) {
        return getSqlFromCache(findByIdSqlCache, clazz, new SqlGenerator() {
            @Override
            public String generate(Class cls) {
                ModelMeta modelMeta = ModelMeta.getModelMeta(cls);
                return findById(modelMeta, false);
            }
        });
    }


    private final static Map<Class, String> findByPartitionIdSqlCache = new ConcurrentHashMap<>();
    public String getFindByPartitionIdSql(final Class clazz) {
        return getSqlFromCache(findByPartitionIdSqlCache, clazz, new SqlGenerator() {
            @Override
            public String generate(Class cls) {
                ModelMeta modelMeta = ModelMeta.getModelMeta(cls);
                return findById(modelMeta, true);
            }
        });
    }


    private final static Map<Class, String> updateSqlCache = new ConcurrentHashMap<>();
    public String getUpdateSql(final Class clazz) {
        return getSqlFromCache(updateSqlCache, clazz, new SqlGenerator() {
            @Override
            public String generate(Class cls) {
                ModelMeta modelMeta = ModelMeta.getModelMeta(cls);
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

                return sql.toString();
            }
        });
    }


    private final static Map<Class, String> insertSqlCache = new ConcurrentHashMap<>();
    public String getInsertSql(final Class clazz) {
        return getSqlFromCache(insertSqlCache, clazz, new SqlGenerator() {
            @Override
            public String generate(Class cls) {
                ModelMeta modelMeta = ModelMeta.getModelMeta(cls);
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
        });
    }


    private final static Map<Class, String> insertDeleteCache = new ConcurrentHashMap<>();
    public String getDeleteSql(final Class clazz) {

        return getSqlFromCache(insertDeleteCache, clazz, new SqlGenerator() {
            @Override
            public String generate(Class cls) {
                ModelMeta modelMeta = ModelMeta.getModelMeta(cls);
                StringBuilder sql = new StringBuilder("DELETE FROM ");
                sql.append(modelMeta.getTableName());
                sql.append(" WHERE ");
                generatePartitionCondition(modelMeta, sql);
                sql.append(modelMeta.getIdColumnMeta().columnName);
                sql.append("=?");
                return sql.toString();
            }
        });
    }




    private String getSqlFromCache(Map<Class, String> cache, Class clazz, SqlGenerator generator) {
        String sql = cache.get(clazz);
        if (sql == null) {
            sql = generator.generate(clazz);
            cache.put(clazz, sql);
        }
        return sql;
    }

    private interface SqlGenerator {
        String generate(Class cls);
    }

    private String findById(ModelMeta modelMeta, boolean partition) {
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

    private void generatePartitionCondition(ModelMeta modelMeta, StringBuilder sql) {
        if (modelMeta.getPartitionColumn() != null) {
            sql.append(modelMeta.getPartitionColumn().columnName);
            sql.append("=? and ");
        }
    }


}
