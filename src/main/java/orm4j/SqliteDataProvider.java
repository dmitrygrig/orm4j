package orm4j;

import SQLite.Database;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import orm4j.query.ILogger;
import orm4j.query.INamedQuery;
import orm4j.query.INamedQueryManager;
import orm4j.query.NamedQuery;
import orm4j.query.NamedQueryParameter;
import orm4j.query.QueryLogger;

public class SqliteDataProvider implements IDataProvider {

    public static final int SQL_EXCEPTION = 4;
    public static final int SQL_BUSY_EXCEPTION = 5; // transaction unavailable
    private boolean IN_TRANSACTION = false;
    private boolean DEBUG = false;
    private Connection transactionConnection = null;
    private String dbPath;
    private INamedQueryManager queryManager;
    private ILogger qlog = new QueryLogger();

    public SqliteDataProvider() {
    }

    public SqliteDataProvider(INamedQueryManager queryManager) {
        this.queryManager = queryManager;
    }

    public SqliteDataProvider(INamedQueryManager queryManager, String databasePath) {
        this.queryManager = queryManager;
        this.dbPath = databasePath;
    }

    public void TestConnection() {
        Connection conn = null;
        Database db = null;
        try {
            // create connection
            Class.forName("SQLite.JDBCDriver").newInstance();
            conn = DriverManager.getConnection("jdbc:sqlite:/C:/SQLite/ilam.db");


            java.lang.reflect.Method m =
                    conn.getClass().getMethod("getSQLiteDatabase", null);
            db = (SQLite.Database) m.invoke(conn, null);
            ResultSet resultSet = null;
            Statement statement = null;
            statement = conn.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM Artist");
            while (resultSet.next()) {
                System.out.println("ARTIST NAME:"
                        + resultSet.getString("artistName"));

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setDebug(boolean debug) {
        this.DEBUG = debug;
    }
    
    @Override
    public ILogger getLogger() {
        return this.qlog;
    }

    @Override
    public MethodResult add(Class<? extends EntityObject> c, IEntityObject obj) {
        return executeObject(c, obj, INamedQuery.NAMED_QUERY_ADD);
    }

    @Override
    public MethodResult update(Class<? extends EntityObject> c, IEntityObject obj, NamedQueryParameter... parameters) {
        return executeObject(c, obj, INamedQuery.NAMED_QUERY_UPDATE);
    }

    @Override
    public MethodResult delete(Class<? extends EntityObject> c, IEntityObject obj, NamedQueryParameter... parameters) {
        return executeObject(c, obj, INamedQuery.NAMED_QUERY_DELETE);
    }

    @Override
    public IEntityObject findSingle(Class<? extends EntityObject> c, String NamedQueryName, NamedQueryParameter... parameters) {
        List<IEntityObject> objList = getObjects(c, NamedQueryName, parameters);
        return objList.isEmpty() ? null : objList.get(0);
    }

    @Override
    public List<IEntityObject> findMany(Class<? extends EntityObject> c, String NamedQueryName, NamedQueryParameter... parameters) {
        return getObjects(c, NamedQueryName, parameters);
    }

    @Override
    public IEntityObject findSingle(Class<? extends EntityObject> c, NamedQuery query) {
        List<IEntityObject> objList = getObjects(c, query);
        return objList.isEmpty() ? null : objList.get(0);
    }

    @Override
    public List<IEntityObject> findMany(Class<? extends EntityObject> c, NamedQuery query) {
        return getObjects(c, query);
    }

    @Override
    public MethodResult beginTransaction() {
        this.IN_TRANSACTION = true;
        return this.ExecuteNonQuery(INamedQuery.NAMED_QUERY_TRANSACTION);
    }

    @Override
    public MethodResult commit() {
        MethodResult res = this.ExecuteNonQuery(INamedQuery.NAMED_QUERY_COMMIT);
        this.IN_TRANSACTION = false;
        this.closeConnection(transactionConnection);
        transactionConnection = null;
        return res;
    }

    @Override
    public MethodResult rollback() {
        MethodResult res = this.ExecuteNonQuery(INamedQuery.NAMED_QUERY_ROLLBACK);
        this.IN_TRANSACTION = false;
        this.closeConnection(transactionConnection);
        transactionConnection = null;
        return res;
    }

    @Override
    public List<IEntityObject> ExecuteQuery(Class<? extends EntityObject> c, NamedQuery query) {
        Connection conn = null;
        List<IEntityObject> res = new ArrayList<IEntityObject>();

        try {
            if ((conn = this.getConnection()) != null) {
                Statement statement = conn.createStatement();
                String queryString = query.getFinalString();
                if (DEBUG) {
                    qlog.log(queryString);
                }
                ResultSet rs = statement.executeQuery(queryString);

                while (rs.next()) {
                    IEntityObject item = (IEntityObject) c.newInstance();
                    item.setObject(rs);

                    res.add(item);
                }

                statement.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            closeConnection(conn);
        }

        return res;
    }

    @Override
    public MethodResult ExecuteNonQuery(String NamedQueryName) {
        NamedQuery nq = new NamedQuery(queryManager.getNamedQuery(NamedQueryName));
        if (nq != null) {
            return this.ExecuteNonQuery(nq);
        }

        return new MethodResult(ARGUMENT_EXCEPTION);
    }

    @Override
    public MethodResult ExecuteNonQuery(NamedQuery query) {
        MethodResult res = new MethodResult();
        Connection conn = null;
        try {
            if ((conn = this.getConnection()) != null) {
                Statement statement = conn.createStatement();
                String queryString = query.getFinalString();
                if (DEBUG) {
                    qlog.log(queryString);
                }
                statement.executeUpdate(queryString);

                if (query.getName().contains(INamedQuery.NAMED_QUERY_ADD)) {
                    res.addValue("id", this.getLastInsertRowId(statement));
                }

                statement.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            res.setException(SQL_EXCEPTION);
        } catch (NullPointerException e) {
            e.printStackTrace();
            res.setException(ARGUMENT_EXCEPTION);
        } catch (Exception e) {
            e.printStackTrace();
            res.setException(INTERNAL_EXCEPTION);
        } finally {
            MethodResult closeRes = closeConnection(conn);
            if (!closeRes.isSuccessful()) {
                res.setException(closeRes.getException());
            }
        }

        return res;
    }

    private MethodResult closeConnection(Connection conn) {
        return closeConnection(conn, false);
    }

    private MethodResult closeConnection(Connection conn, boolean force) {
        MethodResult res = new MethodResult();
        if (conn != null && (!IN_TRANSACTION || force)) {
            try {
                conn.close();
            } catch (SQLException ex) {
                Logger.getLogger(SqliteDataProvider.class.getName()).log(Level.SEVERE, null, ex);
                res.setException(SQL_EXCEPTION);
            }
        }
        return res;
    }

    @Override
    public Connection getConnection() {
        // if transaction is performing the same connection should be used
        if (IN_TRANSACTION) {
            if (transactionConnection == null) {
                transactionConnection = getConnectionObject();
            }

            return transactionConnection;
        }

        return getConnectionObject();
    }

    public Connection getConnectionObject() {
        Connection conn = null;
        try {
            // create Connection
            Class.forName("SQLite.JDBCDriver").newInstance();
            conn = DriverManager.getConnection(String.format("jdbc:sqlite:/%s", getDatabaseURL()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }

    @Override
    public String getDatabaseURL() {
        return dbPath;
    }

    public void setDatabaseURL(String databasePath) {
        this.dbPath = databasePath;
    }

    private ResultSet getResultSet(Statement statement, String query, Object... args) throws SQLException {
        return statement.executeQuery(String.format(query, args));
    }

    private MethodResult executeObject(Class<? extends EntityObject> c, IEntityObject obj, String NamedQueryName) {
        NamedQuery nq = new NamedQuery(queryManager.getNamedQuery(c, NamedQueryName));
        if (nq != null) {
            nq.setParameters(obj.getNamedQueryParameters(true, false));
            return this.ExecuteNonQuery(nq);
        }

        return new MethodResult(ARGUMENT_EXCEPTION);
    }

    private List<IEntityObject> getObjects(Class<? extends EntityObject> c, String NamedQueryName, NamedQueryParameter... parameters) {
        NamedQuery nq = new NamedQuery(queryManager.getNamedQuery(c, NamedQueryName));
        if (nq != null) {
            if (parameters != null) {
                nq.setParameters(parameters);
            }
            return getObjects(c, nq);
        }

        return null;
    }

    private List<IEntityObject> getObjects(Class<? extends EntityObject> c, NamedQuery query) {
        if (query != null) {
            return this.ExecuteQuery(c, query);
        }

        return null;
    }

    private int getLastInsertRowId(Statement statement) throws SQLException {
        // get inserted id
        ResultSet rs = this.getGeneratedKeys(statement);
        while (rs.next()) {
            return Integer.parseInt(rs.getString("last_insert_rowid()"));
        }

        return -1;
    }

    private ResultSet getGeneratedKeys(Statement statement) throws SQLException {
        String queryString = "select last_insert_rowid();";
        if (DEBUG) {
            qlog.log(queryString);
        }
        return getResultSet(statement, queryString, (Object) null);
    }

    @Override
    public void dispose() {
        this.closeConnection(transactionConnection);
    }
}
