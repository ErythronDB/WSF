package org.gusdb.wdk.model.implementation;


import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.QueryInstance;
import org.gusdb.wdk.model.RecordClass;
import org.gusdb.wdk.model.ResultList;
import org.gusdb.wdk.model.ResultFactory;
import org.gusdb.wdk.model.RDBMSPlatformI;
import org.gusdb.wdk.model.WdkLogManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.Collection;

public class SqlQueryInstance extends QueryInstance  {

    private static final Logger logger = WdkLogManager.getLogger("org.gusdb.wdk.model.implementation.SqlQueryInstance");
    
    // ------------------------------------------------------------------
    // Instance Variables
    // ------------------------------------------------------------------

    /**
     * The unique name of the table in a database namespace which holds the cached 
     * results for this Instance.
     */
    String resultTableName = null;

    // ------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------
    
    public SqlQueryInstance (SqlQuery query) {
        super(query);
        logger.finest("I've got a new sqlQueryInstance being created");
    }

    /**
     * @return Sql to run.  If join mode, it is modified for joining
     * @throws WdkModelException
     */
    public String getSql() throws WdkModelException {
	return joinMode?
	    getJoinSql() :
	    ((SqlQuery)query).instantiateSql(query.getInternalParamValues(values));

    }


    private String getJoinSql()  throws WdkModelException {
	String sql = ((SqlQuery)query).getSql();

	SqlClause clause = new SqlClause(sql, joinTableName, 
					 startIndex, endIndex);

	return instantiateSqlWithJoin(clause.getModifiedSql());

    }

    private String instantiateSqlWithJoin(String sql) throws WdkModelException { 
	String primaryKeyJoin = joinTableName + "." + primaryKeyColumnName;

	values.put(RecordClass.PRIMARY_KEY_NAME, primaryKeyJoin); 
	
	if (projectColumnName != null) {
	    String projectJoin = joinTableName + "." + projectColumnName;
	    values.put(RecordClass.PROJECT_ID_NAME, projectJoin);
	}
	return ((SqlQuery)query).instantiateSql(query.getInternalParamValues(values), sql);
    }

    /**
     * @return Full name of table containing result
     */
    public String getResultAsTableName() throws WdkModelException {
        if (resultTableName == null) 
            resultTableName = getResultFactory().getResultAsTableName(this);
        return resultTableName;
    }
    

    public ResultList getResult() throws WdkModelException {

	SqlQuery q = (SqlQuery)query;
        ResultList rl = getResultFactory().getResult(this);
        rl.checkQueryColumns(q, true, getIsCacheable() || joinMode);
        return rl;
    }

    public ResultList getPersistentResultPage(int startRow, int endRow) throws WdkModelException {
	
	if (!getIsCacheable()) throw new WdkModelException("Attempting to get persistent result page, but query instance is not cacheable");

	SqlQuery q = (SqlQuery)query;
        ResultList rl = getResultFactory().getPersistentResultPage(this,
								   startRow,
								   endRow);
        rl.checkQueryColumns(q, true, true);
        return rl;
	
    }

    public String getSqlForCache() throws WdkModelException{
	SqlQuery q = (SqlQuery)query;
	String cacheSql = q.getResultFactory().getSqlForCache(this);
	return cacheSql;
    }

    public Collection getCacheValues() throws WdkModelException{
	return getValues();
    }

    public String getLowLevelQuery() throws WdkModelException {
	return getSql();
    }

    protected ResultList getNonpersistentResult() throws WdkModelException {
        ResultSet resultSet = null;
        RDBMSPlatformI platform = ((SqlQuery)query).getRDBMSPlatform();

        try {
            resultSet = SqlUtils.getResultSet(platform.getDataSource(), 
                    getSql());

        } catch (SQLException e) { 
            String newline = System.getProperty( "line.separator" );
            String msg = newline + "Failed running query:" + newline +
            "\"" + getSql() + "\"" + newline;
            throw new WdkModelException(msg, e);
        }
        return new SqlResultList(this, null, resultSet);
    }

    protected void writeResultToTable(String resultTableName, 
            ResultFactory rf) throws WdkModelException {
        RDBMSPlatformI platform = ((SqlQuery)query).getRDBMSPlatform();

        try {
            platform.createResultTable(platform.getDataSource(),
				       resultTableName, 
				       getSql());
        } catch (SQLException e) {
            throw new WdkModelException(e);
        }
    }

}
