/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package orm4j;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import orm4j.annotation.AnnotationManager;
import orm4j.annotation.AutoIncremented;
import orm4j.annotation.Entity;
import orm4j.annotation.EntityId;
import orm4j.annotation.ForeignKey;
import orm4j.annotation.ManyToMany;
import orm4j.annotation.OneToMany;
import orm4j.annotation.RelationEntity;
import orm4j.exception.DataProviderAddingException;
import orm4j.exception.DataProviderFetchingException;
import orm4j.exception.ForeignKeyException;
import orm4j.query.INamedQueryManager;
import orm4j.query.NamedQuery;
import orm4j.query.NamedQueryParameter;

/**
 *
 * @author grig
 */
public class DataContext implements IDataContext {

    public static final int NO_EXCEPTION = 1;
    public static final int ARGUMENT_EXCEPTION = 2;
    public static final int INTERNAL_EXCEPTION = 3;
    public static final int FOREIGNKEY_EXCEPTION = 4;
    public static final int TRANSACTION_NOT_SPECIFIED_EXCEPTION = 5;
    public static final int TRANSACTION_ALREADY_SPECIFIED_EXCEPTION = 6;
    public boolean DEBUG = false;
    public boolean IN_TRANSACTION = false;
    private IDataProvider dataProvider;
    private INamedQueryManager queryManager;

    public DataContext() {
    }

    public DataContext(IDataProvider dataProvider, INamedQueryManager queryManager) {
        this.dataProvider = dataProvider;
        this.queryManager = queryManager;
    }

    @Override
    public void setDebug(boolean debugMode) {
        dataProvider.setDebug(debugMode);
        this.DEBUG = debugMode;
    }

    @Override
    public void testConnection() {
        // ensure database path is set correctly
        File file = new File(dataProvider.getDatabaseURL());
        if (!file.exists()) {
            System.out.println("Database URL '" + dataProvider.getDatabaseURL() + "' not found.");
        }
        // test all mappings
        for (Class<? extends EntityObject> c : queryManager.getRegisteredClasses()) {
            try {
                List<IEntityObject> obj = (List<IEntityObject>) (List<?>) this.findAll(c);
                if (obj == null) {
                    throw new Exception("Class " + c.getName() + "registered incorrectly.");
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Override
    public MethodResult add(IEntityObject obj) {
        return this.addEntity(obj, true);
    }

    @Override
    public MethodResult update(IEntityObject obj, NamedQueryParameter... parameters) {
        return this.updateEntity(obj, parameters);
    }

    @Override
    public MethodResult delete(IEntityObject obj, NamedQueryParameter... parameters) {
        return dataProvider.delete((Class<? extends EntityObject>) obj.getClass(), obj, parameters);
    }

    @Override
    public <T extends EntityObject> T findSingle(Class<T> clazz, String NamedQueryName, NamedQueryParameter... parameters) {
        return (T) dataProvider.findSingle(clazz, NamedQueryName, parameters);
    }

    @Override
    public <T extends EntityObject> List<T> findMany(Class<T> clazz, String NamedQueryName, NamedQueryParameter... parameters) {
        return (List<T>) (List<?>) dataProvider.findMany(clazz, NamedQueryName, parameters);
    }

    @Override
    public <T extends EntityObject> T findSingle(Class<T> clazz, NamedQuery query) {
        return (T) dataProvider.findSingle(clazz, query);
    }

    @Override
    public <T extends EntityObject> List<T> findMany(Class<T> clazz, NamedQuery query) {
        return (List<T>) (List<?>) dataProvider.findMany(clazz, query);
    }

    @Override
    public <T extends EntityObject> T findById(Class<T> clazz, Object id) {
        return (T) dataProvider.findSingle(clazz, NamedQuery.NAMED_QUERY_GETBYID,
                new NamedQueryParameter(AnnotationManager.GetEntityIdFieldName(clazz), String.valueOf(id)));
    }

    @Override
    public <T extends EntityObject> List<T> findAll(Class<T> clazz) {
        return (List<T>) (List<?>) dataProvider.findMany(clazz, NamedQuery.NAMED_QUERY_GETALL);
    }

    @Override
    public MethodResult executeNonQuery(String query) {
        return dataProvider.ExecuteNonQuery(query);
    }

    @Override
    public <T extends IEntityObject> T checkUnique(T obj) {
        return (T) dataProvider.findSingle(
                (Class<? extends EntityObject>) obj.getClass(),
                NamedQuery.NAMED_QUERY_UNIQUE,
                obj.getNamedQueryParameters(true, true).toArray(new NamedQueryParameter[]{}));
    }

    @Override
    public synchronized MethodResult beginTransaction() {
        if (IN_TRANSACTION) {
            return new MethodResult(TRANSACTION_ALREADY_SPECIFIED_EXCEPTION);
        }

        MethodResult result = dataProvider.beginTransaction();
        if (result.isSuccessful()) {
            if (DEBUG) {
                System.out.println("begin transction...");
            }
            IN_TRANSACTION = true;
            return result;
        } else {
            if (DEBUG) {
                System.out.printf("begin transaction exception: %d.", result.getException());
            }
            return dataProvider.rollback();
        }
    }

    @Override
    public synchronized MethodResult commit() {
        if (!IN_TRANSACTION) {
            return new MethodResult(TRANSACTION_NOT_SPECIFIED_EXCEPTION);
        }

        MethodResult result = dataProvider.commit();
        if (result.isSuccessful()) {
            if (DEBUG) {
                System.out.println("commit transaction.");
            }
            IN_TRANSACTION = false;
            return result;
        } else {
            if (DEBUG) {
                System.out.printf("commit transaction exception: %d.", result.getException());
            }
            return dataProvider.rollback();
        }
    }

    @Override
    public synchronized MethodResult rollback() {
        if (!IN_TRANSACTION) {
            return new MethodResult(TRANSACTION_NOT_SPECIFIED_EXCEPTION);
        }
        if (DEBUG) {
            System.out.println("rollback transaction.");
        }
        IN_TRANSACTION = false;
        return dataProvider.rollback();
    }

    @Override
    public <T extends IEntityObject> List<T> fetchRelations(List<T> objList) {
        for (IEntityObject obj : objList) {
            obj = this.fetchRelations(obj);
        }

        return (List<T>) (List<?>) objList;
    }

    /*
     * Returns object with fetched relations, if no exception during
     * fetch occurs. Otherwise return input obj.
     * 
     */
    @Override
    public <T extends IEntityObject> T fetchRelations(T obj) {
        IEntityObject objClone = obj;

        try {
            Class<? extends EntityObject> c = (Class<? extends EntityObject>) obj.getClass();

            // just Entities could be parent
            if (!c.isAnnotationPresent(Entity.class)) {
                throw new DataProviderFetchingException(obj.getClass());
            }

            // For each Relation try to fetch related objects
            // firstly OneToMany
            for (Field field : AnnotationManager.getFieldsByAnnotation(c, OneToMany.class)) {
                // get class of related obgect
                Class<? extends EntityObject> relObjClass = field.getAnnotation(OneToMany.class).relatedTo();
                // build query
                NamedQuery fetchQuery = queryManager.getQueryFindOneToMany(c, relObjClass, obj);
                // find object
                List<IEntityObject> relObjList = dataProvider.findMany(relObjClass, fetchQuery);
                // add fetched objects to list
                // remark: onetomany related objects are stored only in lists
                field.set(obj, relObjList);
            }

            // add ManyToMany
            for (Field field : AnnotationManager.getFieldsByAnnotation(c, ManyToMany.class)) {
                ManyToMany ann = field.getAnnotation(ManyToMany.class);
                if (ann.collection().equals(List.class)) {
                    // get class of related obgect
                    Class<? extends EntityObject> relObjClass = field.getAnnotation(ManyToMany.class).relatedTo()[0];
                    // get class of relation
                    Class<? extends EntityObject> relationClass = field.getAnnotation(ManyToMany.class).relatedThrough();
                    // build query
                    NamedQuery fetchQuery = queryManager.getQueryFindManyToManySimple(c, relObjClass, relationClass, obj);
                    // find object
                    List<IEntityObject> relObjList = dataProvider.findMany(relObjClass, fetchQuery);
                    // set value
                    field.set(obj, relObjList);
                } else if (ann.collection().equals(Map.class)) {
                    // get 1 class of related obgect
                    Class<? extends EntityObject> relObjClass1 = field.getAnnotation(ManyToMany.class).relatedTo()[0];
                    // get 2 class of related obgect
                    Class<? extends EntityObject> relObjClass2 = field.getAnnotation(ManyToMany.class).relatedTo()[1];
                    // get class of relation
                    Class<? extends EntityObject> relationClass = field.getAnnotation(ManyToMany.class).relatedThrough();
                    //get query to new object
                    NamedQuery fetchQuery = queryManager.getQueryFindRelationByParent(c, relationClass, obj);
                    //create List of Relation Class
                    List<IEntityObject> relClassList = (List<IEntityObject>) (List<?>) this.findMany(relationClass, fetchQuery);
                    //create object map
                    Map relObjMap = new HashMap<IEntityObject, IEntityObject>();
                    //loop over all relation Objects and find Mapping
                    for (IEntityObject relationObj : relClassList) {
                        //GET 1 RELATED OBJECT
                        //relation with Object1
                        Field fkFieldRelObj1 = AnnotationManager.getFKFieldForEntity(relObjClass1, relationClass);
                        //get name of parent Key
                        String parentKeyObj1 = fkFieldRelObj1.getAnnotation(ForeignKey.class).parentKey();
                        // get ID of related Object 1
                        Object relObjId1 = fkFieldRelObj1.get(relationObj);
                        //get related object
                        IEntityObject relObj1 = this.findSingle(relObjClass1, NamedQuery.NAMED_QUERY_GETBYID,
                                new NamedQueryParameter(parentKeyObj1, String.valueOf(relObjId1)));

                        //GET 2 RELATED OBJECT
                        //relation with Object2
                        Field fkFieldRelObj2 = AnnotationManager.getFKFieldForEntity(relObjClass2, relationClass);
                        //get name of parent Key
                        String parentKeyObj2 = fkFieldRelObj2.getAnnotation(ForeignKey.class).parentKey();
                        // get ID of related Object 2
                        Object relObjId2 = fkFieldRelObj2.get(relationObj);
                        //get related object
                        IEntityObject relObj2 = this.findSingle(relObjClass2, NamedQuery.NAMED_QUERY_GETBYID,
                                new NamedQueryParameter(parentKeyObj2, String.valueOf(relObjId2)));

                        //ADD OBJECT TO MAP
                        relObjMap.put(relObj1, relObj2);
                    }
                    // set map to obj
                    field.set(obj, relObjMap);
                } else {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            }
        } catch (IllegalAccessException e) {
            return (T) objClone;
        } catch (IllegalArgumentException e) {
            return (T) objClone;
        } catch (UnsupportedOperationException e) {
            return (T) objClone;
        } catch (DataProviderFetchingException e) {
            return (T) objClone;
        }

        return obj;
    }

    /*
     * adds entity to db.
     * 
     */
    private MethodResult addEntity(IEntityObject obj, boolean checkUnique) {
        MethodResult res = new MethodResult();
        try {
            Class objClass = obj.getClass();

            // add Entity Object
            if (checkUnique) {
                obj = AddObjectToDB((EntityObject) obj);
                res.addValue("id", obj.getEntityId());
                return res;
            } else {
                res = dataProvider.add(objClass, obj);
                if (!res.isSuccessful()) {
                    throw new DataProviderAddingException(objClass);
                }
                obj.setEntityId(res.getValue("id"));
            }

            // Find all Relations (OneToMany or ManyToMany)
            // For each Relation try to add another entities
            // firstly OneToMany
            for (Field field : AnnotationManager.getFieldsByAnnotation(objClass, OneToMany.class)) {
                List<EntityObject> relObjList = (List<EntityObject>) field.get(obj);
                if (relObjList != null) {
                    // for each object
                    for (EntityObject relObj : relObjList) {
                        relObj.setValueForForeignKeyField((EntityObject) obj);
                        this.AddObjectToDB(relObj);
                    }
                }
            }
            // add ManyToMany
            for (Field field : AnnotationManager.getFieldsByAnnotation(objClass, ManyToMany.class)) {
                ManyToMany ann = field.getAnnotation(ManyToMany.class);

                List<EntityObject> relations = new ArrayList<EntityObject>();
                if (ann.collection().equals(List.class)) {
                    List<EntityObject> relObjList = (List<EntityObject>) field.get(obj);
                    if (relObjList != null) {
                        for (EntityObject relObj : relObjList) {
                            relObj = AddObjectToDB(relObj);

                            // create relation
                            EntityObject relation = ann.relatedThrough().newInstance();
                            relation.setValueForForeignKeyField((EntityObject) obj);
                            relation.setValueForForeignKeyField(relObj);

                            relations.add(relation);
                        }
                    }
                } else if (ann.collection().equals(Map.class)) {
                    Map<EntityObject, EntityObject> relObjMap = (Map<EntityObject, EntityObject>) field.get(obj);
                    if (relObjMap != null) {
                        for (Entry<EntityObject, EntityObject> relEntry : (relObjMap).entrySet()) {
                            EntityObject keyObject = AddObjectToDB(relEntry.getKey());
                            EntityObject valueObject = AddObjectToDB(relEntry.getValue());

                            // create relation
                            EntityObject relation = ann.relatedThrough().newInstance();
                            relation.setValueForForeignKeyField((EntityObject) obj);
                            relation.setValueForForeignKeyField(keyObject);
                            relation.setValueForForeignKeyField(valueObject);

                            relations.add(relation);

                        }
                    }
                } else {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                // add all relations to db
                for (EntityObject relationItem : relations) {
                    this.AddRelationToDB(relationItem);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(DataContext.class.getName()).log(Level.SEVERE, null, ex);

            res.setException(INTERNAL_EXCEPTION);
            res.addValue("exception", ex);
        }
        return res;
    }

    private void AddRelationToDB(EntityObject relObj)
            throws DataProviderAddingException {
        if (!relObj.getClass().isAnnotationPresent(RelationEntity.class)) {
            throw new DataProviderAddingException(relObj.getClass());
        }

        if (this.checkUnique(relObj) == null) {
            if (!dataProvider.add(relObj.getClass(), relObj).isSuccessful()) {
                throw new DataProviderAddingException(relObj.getClass());
            }
            if (DEBUG) {
                System.out.printf("%s has been successfully added to db\r\n", relObj.toString());
            }
        } else if (DEBUG) {
            System.out.printf("%s is already exists in db\r\n", relObj.toString());

        }
    }

    /*
     * Adds object to DB if it's neccessary (object has no id or id is not autogenerated and is unique).
     * Returns the same object with defined id in db.
     * 
     */
    private EntityObject AddObjectToDB(EntityObject obj)
            throws DataProviderAddingException {

        if (!obj.getClass().isAnnotationPresent(Entity.class)) {
            throw new DataProviderAddingException(obj.getClass());
        }

        boolean generated = true;
        // is id auto generated
        List<Field> idFields = AnnotationManager.getFieldsByAnnotation(obj.getClass(), EntityId.class);
        if (!idFields.isEmpty()) {
            generated = idFields.get(0).isAnnotationPresent(AutoIncremented.class);
        }

        Integer relObjId = (Integer) obj.getEntityId();
        // if relObj are not in DB
        if (relObjId == null || relObjId.equals(0) || !generated) {
            // try to find entity in db
            EntityObject uniqObj = (EntityObject) this.checkUnique(obj);

            if (uniqObj == null) {
                // add Entity
                MethodResult relResult = this.addEntity(obj, false);

                if (!relResult.isSuccessful()) {
                    throw new DataProviderAddingException(obj.getClass());
                }
                if (DEBUG) {
                    System.out.printf("%s has been successfully added to db\r\n", obj.toString());
                }
            } else {
                obj.setEntityId(uniqObj.getEntityId());
                if (DEBUG) {
                    System.out.printf("Entity %s is already exists in db\r\n", obj.toString());
                }
            }
        }

        return obj;

    }

    /*
     * updates entity in db entity to db.
     * 
     */
    private MethodResult updateEntity(IEntityObject obj, NamedQueryParameter... parameters) {
        MethodResult res = new MethodResult();
        try {
            Class c = obj.getClass();
            // add Entity Object
            res = dataProvider.update(c, obj, parameters);
            if (!res.isSuccessful()) {
                throw new DataProviderAddingException(c);
            }

            // Find all Relations (OneToMany or ManyToMany)
            // For each Relation try to add another entities
            // firstly OneToMany
            for (Field field : AnnotationManager.getFieldsByAnnotation(c, OneToMany.class)) {
                List<EntityObject> relObjList = (List<EntityObject>) field.get(obj);
                if (relObjList != null) {
                    // for each object
                    for (EntityObject relObj : relObjList) {
                        relObj.setValueForForeignKeyField((EntityObject) obj);
                        AddOrUpdateObject(relObj, (EntityObject) obj);
                    }
                }
            }
            // add ManyToMany
            for (Field field : AnnotationManager.getFieldsByAnnotation(c, ManyToMany.class)) {
                ManyToMany ann = field.getAnnotation(ManyToMany.class);

                List<EntityObject> relations = new ArrayList<EntityObject>();
                if (ann.collection().equals(List.class)) {
                    List<EntityObject> relObjList = (List<EntityObject>) field.get(obj);
                    if (relObjList != null) {
                        for (EntityObject relObj : relObjList) {
                            relObj = AddOrUpdateObject(relObj, (EntityObject) obj);

                            // create relation
                            EntityObject relation = ann.relatedThrough().newInstance();
                            relation.setValueForForeignKeyField((EntityObject) obj);
                            relation.setValueForForeignKeyField(relObj);

                            relations.add(relation);
                        }
                    }
                } else if (ann.collection().equals(Map.class)) {
                    Map<EntityObject, EntityObject> relObjMap = (Map<EntityObject, EntityObject>) field.get(obj);
                    if (relObjMap != null) {
                        for (Entry<EntityObject, EntityObject> relEntry : (relObjMap).entrySet()) {
                            EntityObject keyObject = AddOrUpdateObject(relEntry.getKey(), (EntityObject) obj);
                            EntityObject valueObject = AddOrUpdateObject(relEntry.getValue(), (EntityObject) obj);

                            // create relation
                            EntityObject relation = ann.relatedThrough().newInstance();
                            relation.setValueForForeignKeyField((EntityObject) obj);
                            relation.setValueForForeignKeyField(keyObject);
                            relation.setValueForForeignKeyField(valueObject);

                            relations.add(relation);

                        }
                    }
                } else {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                // add all relations to db
                for (EntityObject relationItem : relations) {
                    this.AddRelationToDB(relationItem);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(DataContext.class.getName()).log(Level.SEVERE, null, ex);

            res.setException(INTERNAL_EXCEPTION);
            res.addValue("exception", ex);
        }
        return res;
    }

    /*
     * Adds or updates related object into db
     * and return this relatedObject
     */
    private EntityObject AddOrUpdateObject(EntityObject relObj, EntityObject obj)
            throws DataProviderAddingException, ForeignKeyException {
        // check whether it is already in db
        EntityObject fetchedObject = (EntityObject) this.findById(relObj.getClass(), relObj.getEntityId());
        if (fetchedObject == null) { // if not, add
            relObj = this.AddObjectToDB(relObj);
        } else { // otherwise, update with child entities
            this.updateEntity(relObj, (NamedQueryParameter[]) null);
        }

        return relObj;
    }

    public void dispose() {
        if (dataProvider != null) {
            dataProvider.dispose();
        }
    }

    public void saveLog() {
        dataProvider.getLogger().flush();
        dataProvider.getLogger().clearLog();
    }

    public void saveLog(String filename) {
        dataProvider.getLogger().flush(filename);
        dataProvider.getLogger().clearLog();
    }
}
