/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package orm4j;


import java.sql.Connection;
import java.util.List;
import orm4j.interfaces.Disposable;
import orm4j.query.ILogger;
import orm4j.query.NamedQuery;
import orm4j.query.NamedQueryParameter;

/**
 *
 * @author dmitrygrig <mailto:dmitrygrig@gmail.com>
 */
public interface IDataProvider extends Disposable {
    
    public static final int NO_EXCEPTION = 1;
    public static final int ARGUMENT_EXCEPTION = 2;
    public static final int INTERNAL_EXCEPTION = 3;
    
    // db
    public Connection getConnection();
    public String getDatabaseURL();
    public void setDatabaseURL(String databasePath);
    
    // entity methods
    public MethodResult add(Class<? extends EntityObject> c, IEntityObject obj);
    public MethodResult update(Class<? extends EntityObject> c, IEntityObject obj, NamedQueryParameter... parameters);
    public MethodResult delete(Class<? extends EntityObject> c, IEntityObject obj, NamedQueryParameter... parameters);
    public IEntityObject findSingle(Class<? extends EntityObject> c, String NamedQueryName, NamedQueryParameter... parameters);
    public IEntityObject findSingle(Class<? extends EntityObject> c, NamedQuery query);
    public List<IEntityObject> findMany(Class<? extends EntityObject> c, String NamedQueryName, NamedQueryParameter... parameters);
    public List<IEntityObject> findMany(Class<? extends EntityObject> c, NamedQuery query);
    
    public MethodResult beginTransaction();
    public MethodResult commit();
    public MethodResult rollback();
    
    // queries
    public MethodResult ExecuteNamedQuery(NamedQuery query);
    public MethodResult ExecuteNamedQuery(String NamedQueryName);
    public MethodResult ExecuteNonQuery(String rawSql);
    public List<IEntityObject> ExecuteQuery(Class<? extends EntityObject> c, NamedQuery query);
    
    // debug
    public void setDebug(boolean debug);
    public ILogger getLogger();
}
