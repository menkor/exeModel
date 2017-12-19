package org.exemodel.session.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    public static Log logger = LogFactory.getLog(JdbcSession.class);
    private Connection jdbcConnection;
    private JdbcSessionFactory jdbcSessionFactory;
    private AtomicBoolean activeFlag = new AtomicBoolean(false);
    private transient boolean isInBatch = false;
    private transient boolean isInCacheBatch = false;
    private transient PreparedStatement batchStatement;

    public JdbcSession(Connection jdbcConnection) {
        this.jdbcConnection = jdbcConnection;
    }

    public JdbcSession(JdbcSessionFactory jdbcSessionFactory) {
        this.jdbcSessionFactory = jdbcSessionFactory;
    }

    private static ResultSetHandler<List<Object>> getListResultSetHandler(ModelMeta modelMeta) {
        return new JdbcOrmBeanListHandler(modelMeta.getModelCls(), modelMeta);
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
        if (getTransactionNestedLevel() > 0) {
            isInBatch = false;
            isInCacheBatch = false;
            return;
        }
        try {
            activeFlag.set(false);
            if (jdbcConnection != null) {
                jdbcConnection.close();
                jdbcConnection = null;
            }
            if (isInBatch) {
                if (batchStatement != null) {
                    batchStatement.close();
                    batchStatement = null;
                }
                isInBatch = false;
            }
            if (isInCacheBatch) {
                getCache().endBatch();
                isInCacheBatch = false;
            }
        } catch (SQLException e) {
            throw new JdbcRuntimeException(e);
        }
    }

    private void close(PreparedStatement statement) {
        if (getTransactionNestedLevel() > 0) {
            return;
        }
        try {
            if (jdbcConnection != null) {
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
        String sql = modelMeta.getInsertSql();
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
                                Object generatedId = generatedKeysResultSet.getObject(1);
                                idAccessor.setProperty(entity, generatedId);
                            }
                        } finally {
                            generatedKeysResultSet.close();
                        }
                    }
                    // save to Search Engine
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
        if (entity.operateFields() != null) {
            return update(entity, entity.operateFields());
        }
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
        String sql = modelMeta.getUpdateSql();
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

    @Override
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
            FieldAccessor fieldAccessor = FieldAccessor.getFieldAccessor(modelMeta.getModelCls(), column);
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
        String sql = modelMeta.getDeleteSql();
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
    public void startBatch() {
        this.isInBatch = true;
        ICache cache = getCache();
        if (cache != null) {
            this.isInCacheBatch = true;
            cache.startBatch();
        }
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
                    getCache().executeBatch();
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
    public boolean updateBatch(List<? extends ExecutableModel> entities) {
        if (entities == null || entities.size() == 0) {
            return true;
        }
        this.isInBatch = true;
        for (ExecutableModel entity : entities) {
            update(entity);
        }
        int[] res = executeBatch();
        if (res == null) {
            return false;
        }
        int len = res.length;
        close();
        ExecutableModel tmp = entities.get(0);
        ModelMeta modelMeta = ModelMeta.getModelMeta(tmp.getClass());
        if (modelMeta.isCacheable()) {
            getCache().batchUpdate(entities);
        }
        return len == entities.size();
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
                     if(NumberUtil.isUndefined(idAccessor.getProperty(entity))){
                         Object id = entity.generateId();
                         if(id!=null){
                             hasId = true;
                             idAccessor.setProperty(entity,id);
                         }
                     }else{
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
                if(!hasId){
                    try (ResultSet generatedKeysResultSet = batchStatement.getGeneratedKeys()) {
                        int index = getIndexParamBaseOrdinal();
                        for (Object entity : entities) {
                            if (generatedKeysResultSet.next()) {
                                Object value = generatedKeysResultSet.getObject(index);
                                idAccessor.setProperty(entity, value);
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

    @Override
    public boolean deleteBatch(List<? extends ExecutableModel> entities) {
        if (entities == null || entities.size() == 0) {
            return true;
        }
        this.isInBatch = true;
        for (ExecutableModel entity : entities) {
            delete(entity);
        }
        int len = executeBatch().length;
        close();
        ExecutableModel tmp = entities.get(0);
        ModelMeta modelMeta = ModelMeta.getModelMeta(tmp.getClass());
        if (modelMeta.isCacheable()) {
            getCache().batchDelete(entities);
        }
        return len == entities.size();
    }

    /**
     * @param cls         return Type
     * @param partitionId Distributed db partitionId
     */
    @Override
    public <T> T find(Class<?> cls, Object id, Object partitionId) {
        ModelMeta modelMeta = ModelMeta.getModelMeta(cls);
        String sql;
        boolean withPartitionId = false;
        ModelMeta.ModelColumnMeta partitionColumn = modelMeta.getPartitionColumn();
        if (partitionColumn != null) {
            boolean invalidPartitionId = NumberUtil.isUndefined(partitionId);
            if (invalidPartitionId) {
                if (!modelMeta.isCacheable()) {
                    throw new JdbcRuntimeException(
                            "You should have partition column:" + partitionColumn.columnName);
                }
                sql = modelMeta.getFindByIdSql();
            } else {
                sql = modelMeta.getFindByPartitionIdSql();
                withPartitionId = true;
            }
        } else {
            sql = modelMeta.getFindByIdSql();
        }
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = getJdbcConnection().prepareStatement(sql);
            int i = getIndexParamBaseOrdinal();
            if (withPartitionId) {
                preparedStatement.setObject(i++, partitionId);
            }
            preparedStatement.setObject(i, id);
            ResultSetHandler<List<Object>> handler = getListResultSetHandler(modelMeta);
            ResultSet resultSet = preparedStatement.executeQuery();
            List result = handler.handle(resultSet);
            if (result.size() > 0) {
                return (T) result.get(0);
            } else {
                return null;
            }
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
    public <T> T find(Class<?> cls, Object id) {
        return find(cls, id, null);
    }


    @Override
    public <T> T findListByNativeSql(Class<?> cls, String queryString, Object... params) {
        try {
            QueryRunner runner = new QueryRunner();
            ResultSetHandler<List<Object>> handler = getListResultSetHandler(ModelMeta.getModelMeta(cls));
            return (T) runner.query(getJdbcConnection(), queryString, handler, params);
        } catch (SQLException e) {
            throw new JdbcRuntimeException(e);
        } finally {
            close(null);
        }

    }

    public <T> T findList(Class<?> cls, String queryString, Object... params) {
        PreparedStatement preparedStatement = null;
        int i = getIndexParamBaseOrdinal();
        try {
            preparedStatement = getJdbcConnection().prepareStatement(queryString);
            for (Object p : params) {
                preparedStatement.setObject(i++, p);
            }
            ResultSet resultSet = preparedStatement.executeQuery();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public <T> T findOne(Class<?> cls, String queryString, Object... params) {
        PreparedStatement preparedStatement = null;
        int i = getIndexParamBaseOrdinal();
        try {
            preparedStatement = getJdbcConnection().prepareStatement(queryString);
            for (Object p : params) {
                preparedStatement.setObject(i++, p);
            }
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int count = metaData.getColumnCount();
                for (int j = 0; j < count; j++) {
                    String columnName = metaData.getColumnLabel(j + 1);
                    if (null == columnName || 0 == columnName.length()) {
                        columnName = metaData.getColumnName(j + 1);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param queryString should not append (limit ? , ?) string,limit string generate in this method
     * @return list, the total number is set in pagination
     */
    @Override
    public <T> T findListByNativeSql(Class<?> cls, String queryString,
                                     ParameterBindings parameterBindings, Pagination pagination) {
        queryString = queryString.toUpperCase();
        int lastOrderIndex = queryString.lastIndexOf(" ORDER BY");
        int lastRightBracketIndex = queryString.lastIndexOf(")");
        if (lastOrderIndex < lastRightBracketIndex) {
            throw new JdbcRuntimeException("Pagination query need order by one column");
        }

        //查询总量
        if (pagination.isNeedTotal()) {
            int fromIndex = queryString.indexOf(" FROM");
            StringBuilder countSb = new StringBuilder("SELECT COUNT(*)");
            countSb.append(queryString.substring(fromIndex, lastOrderIndex));
            int count = 0;
            List<Integer> counts = findListByNativeSql(Integer.class, countSb.toString(),
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

        //按分页查询列表
        StringBuilder querySb = new StringBuilder(queryString);
        querySb.append(" LIMIT ? , ?");
        parameterBindings.addIndexBinding(pagination.getOffset());
        parameterBindings.addIndexBinding(pagination.getSize());
        List list = findListByNativeSql(cls, querySb.toString(), parameterBindings);

        if (list.size() < pagination.getSize() && pagination.getPage() == 1
                && pagination.getTotal() < list.size()) {
            pagination.setTotal(list.size());
        }
        return (T) list;
    }


    @Override
    public <T> T findOneByNativeSql(Class<?> cls, String queryString, Object... params) {
        try {
            QueryRunner runner = new QueryRunner();
            ResultSetHandler<List<Object>> handler = getListResultSetHandler(ModelMeta.getModelMeta(cls));
            List<Object> result = runner.query(getJdbcConnection(), queryString, handler, params);
            if (result.size() > 0) {
                return (T) result.get(0);
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new JdbcRuntimeException(e);
        } finally {
            close(null);
        }
    }

    @Override
    public <T> T findOneByNativeSql(Class<?> cls, String queryString,
                                    ParameterBindings parameterBindings) {
        return findOneByNativeSql(cls, queryString,
                parameterBindings.getIndexParametersArray() != null ? parameterBindings
                        .getIndexParametersArray() : new Object[0]);
    }

    @Override
    public <T> T findListByNativeSql(Class<?> cls, String queryString,
                                     ParameterBindings parameterBindings) {
        return findListByNativeSql(cls, queryString,
                parameterBindings.getIndexParametersArray() != null ? parameterBindings
                        .getIndexParametersArray() : new Object[0]);
    }


    @Override
    public int executeUpdate(String sql, ParameterBindings parameterBindings) {
        return executeUpdate(sql, parameterBindings.getIndexParametersArray());
    }


    @Override
    public int executeUpdate(String sql, Object[] params) {//TODO warning the dangerous cache update
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
    public void startCacheBatch() {
        ICache cache = getCache();
        if (cache == null) {
            throw new JdbcRuntimeException("Please config the cache first");
        }
        this.isInCacheBatch = true;
        cache.startBatch();
    }

    @Override
    public void executeCacheBatch() {
        getCache().executeBatch();
        this.isInCacheBatch = false;
        getCache().endBatch();
    }

    @Override
    public boolean isInCacheBatch() {
        return this.isInCacheBatch;
    }

    @Override
    public void execute(String sql) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = getJdbcConnection().prepareStatement(sql);
            preparedStatement.execute();
        } catch (SQLException e) {
            throw new JdbcRuntimeException(e);
        }finally {
            close(preparedStatement);
        }

    }
}
