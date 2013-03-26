/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package orm4j.query;

import java.util.List;
import orm4j.EntityObject;
import orm4j.IEntityObject;

/**
 *
 * @author grig
 */
public interface INamedQueryManager {
    public NamedQuery getNamedQuery(String queryName);
    public NamedQuery getNamedQuery(Class<? extends EntityObject> c, String queryName);
    public NamedQuery getQueryFindManyToManySimple(Class<? extends EntityObject> objClass, Class<? extends EntityObject> relObjClass, Class<? extends EntityObject> relationClass, IEntityObject obj);
    public NamedQuery getQueryFindRelationByParent(Class<? extends EntityObject> objClass, Class<? extends EntityObject> relationClass, IEntityObject obj);
    public NamedQuery getQueryFindOneToMany(Class<? extends EntityObject> objClass, Class<? extends EntityObject> relObjClass, IEntityObject obj);
    public void registerClass(String name);
    public void registerClass(Class<? extends EntityObject> c);
    public void registerNamedQuery(NamedQuery query);
    public List<Class> getRegisteredClasses();
    public List<NamedQuery> getRegisteredQueries();
}
