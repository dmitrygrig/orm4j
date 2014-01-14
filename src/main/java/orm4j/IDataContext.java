/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package orm4j;


import orm4j.interfaces.Disposable;
import java.util.List;
import orm4j.query.NamedQuery;
import orm4j.query.NamedQueryParameter;

/**
 *
 * @author grig
 */
public interface IDataContext extends Disposable {

    // entity crud operations
    public MethodResult add(IEntityObject obj);
    public MethodResult update(IEntityObject obj, NamedQueryParameter... parameters);
    public MethodResult delete(IEntityObject obj, NamedQueryParameter... parameters);
    public <T extends EntityObject> T findSingle(Class<T> clazz, String NamedQueryName, NamedQueryParameter... parameters);
    public <T extends EntityObject> T findSingle(Class<T> clazz, NamedQuery query);
    public <T extends EntityObject> T findById(Class<T> clazz, Object id);
    public <T extends EntityObject> List<T> findMany(Class<T> clazz, String NamedQueryName, NamedQueryParameter... parameters);
    public <T extends EntityObject> List<T> findMany(Class<T> clazz, NamedQuery query);
    public <T extends EntityObject> List<T> findAll(Class<T> clazz);
    public <T extends EntityObject> int findCount(Class<T> clazz);
    
    // raw sql
    public MethodResult executeNonQuery(String sql);
    
    // get count
  public int findCount(NamedQuery query);
    
    // fetching operations
    public <T extends IEntityObject> T fetchRelations(T obj);
    public <T extends IEntityObject> List<T> fetchRelations(List<T> objList);

    // check unique
    public <T extends IEntityObject> T checkUnique(T obj);

    // transactions
    public MethodResult beginTransaction();
    public MethodResult commit();
    public MethodResult rollback();
    
    // test
    public void testConnection();
    public void setDebug(boolean debugMode);
    
    // log
    public void saveLog();
    public void saveLog(String filename);
}
