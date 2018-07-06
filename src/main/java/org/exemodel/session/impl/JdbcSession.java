package org.exemodel.session.impl;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.*;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.exemodel.cache.ICache;
import org.exemodel.exceptions.JdbcRuntimeException;
import org.exemodel.orm.ExecutableModel;
import org.exemodel.orm.FieldAccessor;
import org.exemodel.orm.ModelMeta;
import org.exemodel.session.AbstractSession;
import org.exemodel.transation.JdbcTransaction;
import org.exemodel.transation.Transaction;
import org.exemodel.util.NumberUtil;
import org.exemodel.util.Pagination;
import org.exemodel.util.ParameterBindings;
import org.exemodel.util.StringUtil;

@SuppressWarnings("unchecked")
public class JdbcSession extends AbstractSession {

    private Connection jdbcConnection;
    private JdbcSessionFactory jdbcSessionFactory;
    private AtomicBoolean activeFlag = new AtomicBoolean(false);
    private volatile boolean pmdKnownBroken = false;
    private transient PreparedStatement batchStatement;

    public JdbcSession(Connection jdbcConnection) {
        this.jdbcConnection = jdbcConnection;
    }

    public JdbcSession(JdbcSessionFactory jdbcSessionFactory) {
        this.jdbcSessionFactory = jdbcSessionFactory;
    }


    public AtomicBoolean getActiveFlag() {
        return activeFlag;
    }

    public synchronized Connection getJdbcConnection() {
        try {
            if (jdbcConnection == null || jdbcConnection.isClosed()) {
                jdbcConnection = jdbcSessionFactory.createJdbcConnection();
            }
        } catch (Exception e) {
            throw new JdbcRuntimeException(e);
        }

        return jdbcConnection;
    }

    public boolean getAutoCommit() {
        try {
            return getJdbcConnection().getAutoCommit();
        } catch (SQLException e) {
            throw new JdbcRuntimeException(e);
        }
    }

    public void setAutoCommit(boolean autoCommit) {
        try {
            getJdbcConnection().setAutoCommit(autoCommit);
        } catch (SQLException e) {
            throw new JdbcRuntimeException(e);
        }
    }

    @Override
    public Transaction getTransaction() {
        return new JdbcTransaction(this);
    }

    @Override
    public boolean isOpen() {
        try {
            if (jdbcConnection == null) {
                return true;
            }
            return !jdbcConnection.isClosed();
        } catch (SQLException e) {
            throw new JdbcRuntimeException(e);
        }
    }

    @Override
    public void close() {

        try {
            isInBatch = false;
            isInCacheBatch = false;
            if (batchStatement != null) {
                batchStatement.close();
                batchStatement = null;
            }
            endCacheBatch();
            if (getTransactionNestedLevel() == 0) {
                activeFlag.set(false);
                if (jdbcConnection != null) {
                    jdbcConnection.close();
                    jdbcConnection = null;
                }
            }
        } catch (SQLException e) {
            throw new JdbcRuntimeException(e);
        }
    }



    private void close(PreparedStatement statement) {
        try {

            if (getTransactionNestedLevel() == 0 && jdbcConnection != null) {
                jdbcConnection.close();
                jdbcConnection = null;
            }
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            throw new JdbcRuntimeException(e);
        }

    }

    private int setStatementAllField(ModelMeta modelMeta, PreparedStatement preparedStatement,
                                     Object entity, boolean skipId) {
        int i = getIndexParamBaseOrdinal();
        try {
            for (ModelMeta.ModelColumnMeta columnMeta : modelMeta.getColumnMetaSet()) {
                if (skipId && (columnMeta.isId || columnMeta.isPartition)) {
                    continue;
                }
                FieldAccessor fieldAccessor = columnMeta.fieldAccessor;
                Object value = fieldAccessor.getProperty(entity);
                if (columnMeta.isPartition && NumberUtil.isUndefined(value)) {
                    throw new JdbcRuntimeException(
                            "Partition Column's value  can't be null:" + columnMeta.columnName);
                }
                preparedStatement.setObject(i, value);
                i++;

            }
            return i;
        } catch (SQLException e) {
            throw new JdbcRuntimeException(e);
        }

    }

    @Override
    public boolean save(ExecutableModel entity) {
        final ModelMeta modelMeta = ModelMeta.getModelMeta(entity.getClass());
        String sql = modelMeta.sqlGenerator().getInsertSql();
        if (!isInBatch) {
            PreparedStatement preparedStatement = null;
            try {
                preparedStatement = getJdbcConnection()
                        .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                setStatementAllField(modelMeta, preparedStatement, entity, false);
                int changedCount = preparedStatement.executeUpdate();

                if (changedCount < 1) {
                    return false;
                }
                FieldAccessor idAccessor = modelMeta.getIdAccessor();

                if (idAccessor != null) {
                    Object value = idAccessor.getProperty(entity);
                    if (NumberUtil.isUndefined(value)) {
                        ResultSet generatedKeysResultSet = preparedStatement.getGeneratedKeys();
                        try {
                            if (generatedKeysResultSet.next()) {
                                Object generatedId = generatedKeysResultSet.getLong(1);
                                setGeneratedId(idAccessor, entity, generatedId);
                            }
                        } finally {
                            generatedKeysResultSet.close();
                        }
                    }
                }
            } catch (SQLException e) {
                throw new JdbcRuntimeException(e);
            } finally {
                close(preparedStatement);
            }
        } else {
            try {
                if (batchStatement == null) {
                    batchStatement = getJdbcConnection()
                            .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                }
                setStatementAllField(modelMeta, batchStatement, entity, false);
                batchStatement.addBatch();
            } catch (SQLException e) {
                close();
                throw new JdbcRuntimeException(e);
            }
        }
        return true;
    }

    @Override
    public boolean update(final ExecutableModel entity) {

        final ModelMeta modelMeta = ModelMeta.getModelMeta(entity.getClass());
        boolean isPartition = modelMeta.getPartitionColumn() != null;
        Object partitionId = null;

        if (isPartition) {
            partitionId = modelMeta.getPartitionColumn().fieldAccessor.getProperty(entity);
            if (NumberUtil.isUndefined(partitionId)) {
                throw new JdbcRuntimeException("you should update with partition id");
            }
        }
        final FieldAccessor idAccessor = modelMeta.getIdAccessor();
        String sql = modelMeta.sqlGenerator().getUpdateSql();
        if (!isInBatch) {
            PreparedStatement preparedStatement = null;
            try {
                preparedStatement = getJdbcConnection().prepareStatement(sql);
                int i = setStatementAllField(modelMeta, preparedStatement, entity, true);
                Object id = idAccessor.getProperty(entity);
                if (isPartition) {
                    preparedStatement.setObject(i, partitionId);
                    i = i + 1;

                }
                preparedStatement.setObject(i, id);
                boolean res = preparedStatement.executeUpdate() > 0;
                return res;
            } catch (SQLException e) {
                throw new JdbcRuntimeException(e);
            } finally {
                close(preparedStatement);
            }

        } else {
            try {
                if (batchStatement == null) {
                    batchStatement = getJdbcConnection().prepareStatement(sql);
                }
                int i = setStatementAllField(modelMeta, batchStatement, entity, true);

                if (isPartition) {
                    batchStatement.setObject(i, partitionId);
                    i = i + 1;

                }
                Object id = idAccessor.getProperty(entity);
                batchStatement.setObject(i, id);
                batchStatement.addBatch();
                return true;
            } catch (SQLException e) {
                close();
                throw new JdbcRuntimeException(e);
            }
        }
    }

    public boolean update(ExecutableModel entity, String... columns) {
        if (entity == null) {
            return false;
        }
        final ModelMeta modelMeta = ModelMeta.getModelMeta(entity.getClass());
        if (modelMeta.getPartitionColumn() != null) {
            throw new JdbcRuntimeException(" This method don't support partition entity:" +
                    modelMeta.getPartitionColumn().columnName);
        }

        final FieldAccessor idAccessor = modelMeta.getIdAccessor();
        ParameterBindings parameterBindings = new ParameterBindings();
        StringBuilder sb = new StringBuilder(" UPDATE ");
        sb.append(modelMeta.getTableName());
        sb.append(" SET ");
        boolean init = true;
        for (String column : columns) {
            if (init) {
                init = false;
            } else {
                sb.append(",");
            }
            FieldAccessor fieldAccessor = modelMeta.getFieldAccessor(column);
            Object value = fieldAccessor.getProperty(entity);
            sb.append(StringUtil.underscoreName(column));
            sb.append("=? ");
            parameterBindings.addIndexBinding(value);
        }
        sb.append(" WHERE ");
        sb.append(modelMeta.getIdName());
        sb.append("=?");

        parameterBindings.addIndexBinding(idAccessor.getProperty(entity));
        String sql = sb.toString();
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = getJdbcConnection().prepareStatement(sql);
            parameterBindings.appendToStatement(preparedStatement);
            int i = preparedStatement.executeUpdate();
            return i > 0;
        } catch (SQLException e) {
            throw new JdbcRuntimeException(e);
        } finally {
            close(preparedStatement);
        }
    }

    @Override
    public boolean delete(ExecutableModel entity) {
        if (entity == null) {
            return false;
        }
        ModelMeta modelMeta = ModelMeta.getModelMeta(entity.getClass());
        FieldAccessor idAccessor = modelMeta.getIdAccessor();
        boolean isPartitioned = modelMeta.getPartitionColumn() != null;
        Object partitionId = null;
        if (isPartitioned) {
            partitionId = modelMeta.getPartitionColumn().fieldAccessor.getProperty(entity);
            if (NumberUtil.isUndefined(partitionId)) {
                throw new JdbcRuntimeException("You should delete with partition column value:"
                        + modelMeta.getPartitionColumn().columnName);
            }
        }
        String sql = modelMeta.sqlGenerator().getDeleteSql();
        if (!isInBatch) {
            PreparedStatement preparedStatement = null;
            try {
                preparedStatement = getJdbcConnection().prepareStatement(sql);
                int i = getIndexParamBaseOrdinal();
                if (isPartitioned) {
                    preparedStatement.setObject(i, partitionId);
                    i = i + 1;

                }
                Object id = idAccessor.getProperty(entity);
                if (NumberUtil.isUndefined(id)) {
                    throw new JdbcRuntimeException("id should not be null");
                }
                preparedStatement.setObject(i, id);
                return preparedStatement.executeUpdate() > 0;
            } catch (SQLException e) {
                throw new JdbcRuntimeException(e);
            } finally {
                close(preparedStatement);
            }
        } else {
            try {
                if (batchStatement == null) {
                    batchStatement = getJdbcConnection().prepareStatement(sql);
                }
                int i = getIndexParamBaseOrdinal();
                if (isPartitioned) {
                    batchStatement.setObject(i, partitionId);
                    i = i + 1;
                }
                Object id = idAccessor.getProperty(entity);
                batchStatement.setObject(i, id);
                batchStatement.addBatch();
            } catch (SQLException e) {
                close();
                throw new JdbcRuntimeException(e);
            }
        }
        return true;
    }



    @Override
    public int[] executeBatch() {
        if (isInBatch) {
            try {
                int[] res = null;
                if (batchStatement != null) {
                    res = batchStatement.executeBatch();
                }
                if (isInCacheBatch) {
                    executeCacheBatch();
                }
                return res;
            } catch (SQLException e) {
                throw new JdbcRuntimeException(e);
            } finally {
                close();
            }

        } else {
            throw new JdbcRuntimeException("Not in batch");
        }
    }



    @Override
    public boolean saveBatch(List<? extends ExecutableModel> entities) {
        int len = 0;
        if (entities.size() != 0) {
            ExecutableModel tmp = entities.get(0);
            ModelMeta modelMeta = ModelMeta.getModelMeta(tmp.getClass());
            try {
                this.isInBatch = true;
                boolean hasId = false;
                FieldAccessor idAccessor = modelMeta.getIdAccessor();
                for (ExecutableModel entity : entities) {
                    if (NumberUtil.isUndefined(idAccessor.getProperty(entity))) {
                        Object id = entity.generateId();
                        if (id != null) {
                            hasId = true;
                            idAccessor.setProperty(entity, id);
                        }
                    } else {
                        hasId = true;
                    }
                    save(entity);
                }
                int[] res = batchStatement.executeBatch();
                if (res == null) {
                    close();
                    return false;
                }

                len = res.length;
                if (!hasId) {
                    try (ResultSet generatedKeysResultSet = batchStatement.getGeneratedKeys()) {
                        int index = getIndexParamBaseOrdinal();
                        for (Object entity : entities) {
                            if (generatedKeysResultSet.next()) {
                                Object value = generatedKeysResultSet.getObject(index);
                                setGeneratedId(idAccessor, entity, value);
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                throw new JdbcRuntimeException(e.getMessage());
            } finally {
                close();
            }
            if (modelMeta.isCacheable()) {
                getCache().batchSave(entities);
            }
        }
        return len == entities.size();

    }



    /**
     * @param cls         return Type
     * @param partitionId Distributed db partitionId
     */
    @Override
    public <T> T find(Class<? extends T> cls, Object id, Object partitionId) {
        ModelMeta modelMeta = ModelMeta.getModelMeta(cls);
        String sql;
        boolean withPartitionId = false;
        ModelMeta.ModelColumnMeta partitionColumn = modelMeta.getPartitionColumn();
        if (partitionColumn != null) {
            boolean invalidPartitionId = NumberUtil.isUndefined(partitionId);
            if (invalidPartitionId) {
                sql = modelMeta.sqlGenerator().getFindByIdSql();
            } else {
                sql = modelMeta.sqlGenerator().getFindByPartitionIdSql();
                withPartitionId = true;
            }
        } else {
            sql = modelMeta.sqlGenerator().getFindByIdSql();
        }
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = getJdbcConnection().prepareStatement(sql);
            int i = getIndexParamBaseOrdinal();
            if (withPartitionId) {
                preparedStatement.setObject(i++, partitionId);
            }
            preparedStatement.setObject(i, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            return beanProcessor.toBean(resultSet, cls);
        } catch (SQLException e) {
            throw new JdbcRuntimeException(e);
        } finally {
            close(preparedStatement);
        }
    }


    /**
     * 根据id查找model
     */
    @Override
    public <T> T find(Class<? extends T> cls, Object id) {
        return find(cls, id, null);
    }


    @Override
    public <T> List<T> findListByNativeSql(Class<? extends T> cls, String queryString, Object... params) {
        PreparedStatement statement =null;
        try {
            statement = getJdbcConnection().prepareStatement(queryString);
            fillStatement(statement,params);
            ResultSet resultSet = statement.executeQuery();
            return beanProcessor.toBeanList(resultSet, cls);
        } catch (SQLException e) {
            throw new JdbcRuntimeException(e);
        } finally {
            close(statement);
        }
    }

    /**
     * @param queryString should not append (limit ? , ?) string,limit string generate in this method
     * @return list, the total number is set in pagination
     */
    @Override
    public <T> List<T> findListByNativeSql(Class<? extends T> cls, String queryString,
                                           ParameterBindings parameterBindings, Pagination pagination) {
        queryString = queryString.toLowerCase();
        int lastOrderIndex = queryString.lastIndexOf(" order by");
        int lastRightBracketIndex = queryString.lastIndexOf(")");
        if (lastOrderIndex < lastRightBracketIndex) {
            throw new JdbcRuntimeException("Pagination query need order by one column");
        }


        StringBuilder querySb = new StringBuilder(queryString);
        querySb.append(" LIMIT ? , ?");
        ParameterBindings fetchLimitCol = new ParameterBindings();
        fetchLimitCol.extend(parameterBindings);
        fetchLimitCol.addIndexBinding(pagination.getOffset());
        fetchLimitCol.addIndexBinding(pagination.getSize());
        List<T> list = findListByNativeSql(cls, querySb.toString(), fetchLimitCol);

        if (list.size() < pagination.getSize() && pagination.getPage() == 1) {
            pagination.setTotal(list.size());
            return list;
        }

        //fetch total
        if (pagination.isNeedTotal()) {
            int fromIndex = queryString.indexOf(" from");
            StringBuilder countSb = new StringBuilder("SELECT COUNT(*)");
            countSb.append(queryString.substring(fromIndex, lastOrderIndex));
            long count = 0;
            List<Long> counts = findListByNativeSql(Long.class, countSb.toString(),
                    parameterBindings);
            if (counts != null && counts.size() != 0) {
                if (counts.size() > 1) { // with group by
                    count = counts.size();
                } else {
                    count = counts.get(0);
                }
            }
            pagination.setTotal(count);
        } else {
            pagination.setTotal(-1);
        }

        return list;
    }


    @Override
    public <T> T findOneByNativeSql(Class<? extends T> cls, String queryString, Object... params) {
        PreparedStatement statement = null;
        try {
            statement = getJdbcConnection().prepareStatement(queryString);
            fillStatement(statement, params);
            ResultSet resultSet = statement.executeQuery();
            return beanProcessor.toBean(resultSet, cls);
        } catch (SQLException e) {
            throw new JdbcRuntimeException(e);
        } finally {
            close(statement);
        }
    }

    @Override
    public <T> T findOneByNativeSql(Class<? extends T> cls, String queryString,
                                    ParameterBindings parameterBindings) {
        return findOneByNativeSql(cls, queryString,
                parameterBindings.getIndexParametersArray() != null ? parameterBindings.getIndexParametersArray() : new Object[0]);
    }

    @Override
    public <T> List<T> findListByNativeSql(Class<? extends T> cls, String queryString,
                                           ParameterBindings parameterBindings) {
        return findListByNativeSql(cls, queryString,
                parameterBindings.getIndexParametersArray() != null ? parameterBindings.getIndexParametersArray() : new Object[0]);
    }

    @Override
    public <T> T callProcedure(Class<? extends T> DO, String callString, ParameterBindings parameterBindings) {
        CallableStatement callableStatement = null;
        try {
            callableStatement = getConnection().prepareCall(callString);
            fillStatement(callableStatement, parameterBindings.getIndexParametersArray());
            T result = DO.newInstance();
            Field[] fields = DO.getDeclaredFields();
            int i = 0;
            boolean hasRs = callableStatement.execute();
            while (hasRs) {
                ResultSet resultSet = callableStatement.getResultSet();
                Field field = fields[i++];
                field.setAccessible(true);
                if (Collection.class.isAssignableFrom(field.getType())) {
                    ParameterizedType type = (ParameterizedType) field.getGenericType();
                    Class<?> genericType = (Class<?>) type.getActualTypeArguments()[0];
                    field.set(result, beanProcessor.toBeanList(resultSet, genericType));
                } else {
                    field.set(result, beanProcessor.toBean(resultSet, field.getType()));
                }
                hasRs = callableStatement.getMoreResults();
            }

            return result;
        } catch (SQLException | InstantiationException | IllegalAccessException e) {
            throw new JdbcRuntimeException(e);
        } finally {
            close(callableStatement);
        }
    }

    @Override
    public int executeUpdate(String sql, ParameterBindings parameterBindings) {
        return executeUpdate(sql, parameterBindings.getIndexParametersArray());
    }

    //TODO warning the dangerous cache update
    @Override
    public int executeUpdate(String sql, Object[] params) {
        if (!isInBatch) {
            PreparedStatement preparedStatement = null;
            try {
                preparedStatement = getJdbcConnection().prepareStatement(sql);
                if (params != null && params.length > 0) {
                    int i = getIndexParamBaseOrdinal();
                    for (Object p : params) {
                        preparedStatement.setObject(i, p);
                        i++;
                    }
                }
                return preparedStatement.executeUpdate();
            } catch (SQLException e) {
                throw new JdbcRuntimeException(e);
            } finally {
                close(preparedStatement);
            }
        } else {
            try {
                if (batchStatement == null) {
                    batchStatement = getJdbcConnection().prepareStatement(sql);
                }
                if (params != null && params.length > 0) {
                    int i = getIndexParamBaseOrdinal();
                    for (Object p : params) {
                        batchStatement.setObject(i, p);
                        i++;
                    }
                    batchStatement.addBatch();
                }
                return 1;
            } catch (SQLException e) {
                close();
                throw new JdbcRuntimeException(e);
            }
        }
    }




    @Override
    public boolean execute(String sql) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = getJdbcConnection().prepareStatement(sql);
            return preparedStatement.execute();
        } catch (SQLException e) {
            throw new JdbcRuntimeException(e);
        } finally {
            close(preparedStatement);
        }
    }



    private void setGeneratedId(FieldAccessor idAccessor, Object entity, Object id) {
        Class<?> type = idAccessor.getPropertyType();
        if (id instanceof Number) {
            if (type == Long.TYPE || type == Long.class) {
                idAccessor.setProperty(entity, ((Number) id).longValue());
            } else if (type == Integer.TYPE || type == Integer.class) {
                idAccessor.setProperty(entity, ((Number) id).intValue());
            } else {
                idAccessor.setProperty(entity, id);
            }
        } else {
            idAccessor.setProperty(entity, id);
        }
    }


    /**
     * @param stmt
     *            PreparedStatement to fill
     * @param params
     *            Query replacement parameters; <code>null</code> is a valid
     *            value to pass in.
     * @throws SQLException
     *             if a database access error occurs
     */
    private void fillStatement(PreparedStatement stmt, Object... params)
            throws SQLException {

        // check the parameter count, if we can
        ParameterMetaData pmd = null;
        if (!pmdKnownBroken) {
            try {
                pmd = stmt.getParameterMetaData();
                if (pmd == null) { // can be returned by implementations that don't support the method
                    pmdKnownBroken = true;
                } else {
                    int stmtCount = pmd.getParameterCount();
                    int paramsCount = params == null ? 0 : params.length;
                    if (stmtCount != paramsCount) {
                        throw new SQLException("Wrong number of parameters: expected "
                                + stmtCount + ", was given " + paramsCount);
                    }
                }
            } catch (SQLFeatureNotSupportedException ex) {
                pmdKnownBroken = true;
            }
        }

        // nothing to do here
        if (params == null) {
            return;
        }
        for (int i = 0; i < params.length; i++) {
            if (params[i] != null) {
                stmt.setObject(i + 1, params[i]);
            } else {
                // VARCHAR works with many drivers regardless
                // of the actual column type. Oddly, NULL and
                // OTHER don't work with Oracle's drivers.
                int sqlType = Types.VARCHAR;
                if (!pmdKnownBroken) {
                    try {
                        /*
                         * It's not possible for pmdKnownBroken to change from
                         * true to false, (once true, always true) so pmd cannot
                         * be null here.
                         */
                        sqlType = pmd.getParameterType(i + 1);
                    } catch (SQLException e) {
                        pmdKnownBroken = true;
                    }
                }
                stmt.setNull(i + 1, sqlType);
            }
        }
    }

}
