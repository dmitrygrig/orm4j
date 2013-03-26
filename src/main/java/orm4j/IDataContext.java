/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package orm4j;


import orm4j.interfaces.Disposable;
import java.util.List;
import orm4j.exception.DataContextTransactionException;
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
    public IEntityObject findSingle(Class<? extends EntityObject> c, String NamedQueryName, NamedQueryParameter... parameters);
    public IEntityObject findSingle(Class<? extends EntityObject> c, NamedQuery query);
    public IEntityObject findById(Class<? extends EntityObject> c, Object id);
    public List<IEntityObject> findMany(Class<? extends EntityObject> c, String NamedQueryName, NamedQueryParameter... parameters);
    public List<IEntityObject> findMany(Class<? extends EntityObject> c, NamedQuery query);
    public List<IEntityObject> findAll(Class<? extends EntityObject> c);

    // fetching operaions
    public IEntityObject fetchRelations(IEntityObject obj);
    public List<IEntityObject> fetchRelations(List<IEntityObject> objList);

    // check unique
    public IEntityObject checkUnique(IEntityObject obj);

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
