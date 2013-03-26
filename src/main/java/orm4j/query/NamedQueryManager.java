/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package orm4j.query;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import orm4j.EntityObject;
import orm4j.IEntityObject;
import orm4j.annotation.AnnotationManager;
import orm4j.annotation.Column;
import orm4j.annotation.Entity;
import orm4j.annotation.ForeignKey;
import orm4j.annotation.RelationEntity;
import orm4j.annotation.Table;
import orm4j.annotation.Unique;
import orm4j.exception.UniqueException;

/**
 *
 * @author grig
 */
public class NamedQueryManager implements INamedQueryManager {

    private List<NamedQuery> queries;
    private List<Class> registeredClasses;

    public NamedQueryManager() {
        queries = new ArrayList<NamedQuery>();
        registeredClasses = new ArrayList<Class>();

        initServiceQueries();
    }

    private void initServiceQueries() {
        registerNamedQuery(new NamedQuery(INamedQuery.NAMED_QUERY_TRANSACTION, "begin transaction"));
        registerNamedQuery(new NamedQuery(INamedQuery.NAMED_QUERY_COMMIT, "commit"));
        registerNamedQuery(new NamedQuery(INamedQuery.NAMED_QUERY_ROLLBACK, "rollback"));
    }

    @Override
    public void registerClass(Class<? extends EntityObject> c) {
        if (!registeredClasses.contains(c)) {
            if (c.isAnnotationPresent(Table.class)) {
                if (c.isAnnotationPresent(Entity.class)) {
                    queries.addAll(Arrays.asList(
                            getQueryFindAll(c),
                            getQueryFindById(c),
                            getQueryFindByName(c),
                            getQueryInsert(c),
                            getQueryUpdate(c),
                            getQueryDelete(c),
                            getQueryUnique(c)));
                } else if (c.isAnnotationPresent(RelationEntity.class)) {
                    queries.addAll(Arrays.asList(
                            getQueryFindAll(c),
                            getQueryInsert(c),
                            getQueryDelete(c),
                            getQueryUnique(c)));
                }
            }
            registeredClasses.add(c);
        }
    }

    @Override
    public void registerClass(String name) {
        try {
            Class<? extends EntityObject> c = (Class<? extends EntityObject>) ClassLoader.getSystemClassLoader().loadClass(name);
            if (c != null) {
                registerClass(c);
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(NamedQueryManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void registerNamedQuery(NamedQuery query) {
        queries.add(query);
    }

    @Override
    public List<Class> getRegisteredClasses() {
        return registeredClasses;
    }

    @Override
    public List<NamedQuery> getRegisteredQueries() {
        return queries;
    }

    @Override
    public NamedQuery getNamedQuery(String queryName) {
        for (NamedQuery nq : queries) {
            if (nq.getName().equals(queryName)) {
                return nq;
            }
        }

        return null;
    }

    @Override
    public NamedQuery getNamedQuery(Class<? extends EntityObject> c, String queryName) {
        String searchQueryName = getQueryName(c, queryName);
        return getNamedQuery(searchQueryName);
    }

    private String getQueryName(Class<? extends EntityObject> c, String queryName) {
        return getTableName(c).concat(".").concat(queryName);
    }

    private NamedQuery getQueryFindById(Class<? extends EntityObject> c) {
        String tableName = getTableName(c);
        String fieldIdName = AnnotationManager.GetEntityIdFieldName(c);

        return new NamedQuery(getQueryName(c, INamedQuery.NAMED_QUERY_GETBYID),
                String.format("SELECT * FROM %s WHERE %s = ':%s'", tableName, fieldIdName, fieldIdName));
    }

    private NamedQuery getQueryFindByName(Class<? extends EntityObject> c) {
        String tableName = getTableName(c);
        String fieldIdName = AnnotationManager.GetEntityNameFieldName(c);

        return new NamedQuery(getQueryName(c, INamedQuery.NAMED_QUERY_GETBYNAME),
                String.format("SELECT * FROM %s WHERE %s = ':%s'", tableName, fieldIdName, fieldIdName));
    }

    private NamedQuery getQueryFindAll(Class<? extends EntityObject> c) {
        String tableName = getTableName(c);

        return new NamedQuery(getQueryName(c, INamedQuery.NAMED_QUERY_GETALL),
                String.format("SELECT * FROM %s", tableName));
    }

    @Override
    public NamedQuery getQueryFindOneToMany(
            Class<? extends EntityObject> objClass,
            Class<? extends EntityObject> relObjClass,
            IEntityObject obj) {

        String parentIdFieldName = AnnotationManager.GetEntityIdFieldName(objClass);
        String parentTableName = AnnotationManager.getEntityTableName(objClass);
        String childTableName = AnnotationManager.getEntityTableName(relObjClass);
        Field fkField = AnnotationManager.getFKFieldForEntity(objClass, relObjClass);
        String fkChildColumnName = fkField.getAnnotation(Column.class).name();
        String fkParentColumnName = fkField.getAnnotation(ForeignKey.class).parentKey();

        return new NamedQuery("Select", String.format("Select c.* from %s c, %s p where c.%s = p.%s and p.%s = %s",
                childTableName, // child table
                parentTableName, // parent table
                fkChildColumnName, // child foreign key column
                fkParentColumnName, // parent foreign key column
                parentIdFieldName, // parent id column name
                String.valueOf(obj.getEntityId()) // parent id value
                ));
    }

    @Override
    public NamedQuery getQueryFindManyToManySimple(
            Class<? extends EntityObject> objClass,
            Class<? extends EntityObject> relObjClass,
            Class<? extends EntityObject> relationClass,
            IEntityObject obj) {

        String parentIdFieldName = AnnotationManager.GetEntityIdFieldName(objClass);
        String parentTableName = AnnotationManager.getEntityTableName(objClass);
        String relationTableName = AnnotationManager.getEntityTableName(relationClass);
        String childTableName = AnnotationManager.getEntityTableName(relObjClass);
        Field fkFieldChildRelation = AnnotationManager.getFKFieldForEntity(relObjClass, relationClass);
        Field fkFieldParentRelation = AnnotationManager.getFKFieldForEntity(objClass, relationClass);
        String fkChildColumnName = fkFieldChildRelation.getAnnotation(ForeignKey.class).parentKey();
        String fkRelationToChildColumnName = fkFieldChildRelation.getAnnotation(Column.class).name();
        String fkRelationToParentColumnName = fkFieldParentRelation.getAnnotation(Column.class).name();
        String fkParentColumnName = fkFieldParentRelation.getAnnotation(ForeignKey.class).parentKey();

        return new NamedQuery("Select", String.format("Select c.* from %s c inner join %s r on r.%s = c.%s inner join %s p on r.%s = p.%s where p.%s = %s",
                childTableName, // child table
                relationTableName, // relation table
                fkRelationToChildColumnName, // relation to child foreign key column
                fkChildColumnName, // child foreign key column
                parentTableName, // parent table
                fkRelationToParentColumnName, // relation to parent foreign key column
                fkParentColumnName, // parent foreign key column
                parentIdFieldName, // parent id column name
                String.valueOf(obj.getEntityId()) // parent id value
                ));
    }

    public NamedQuery getQueryFindRelationByParent(
            Class<? extends EntityObject> objClass,
            Class<? extends EntityObject> relationClass,
            IEntityObject obj) {

        String parentIdFieldName = AnnotationManager.GetEntityIdFieldName(objClass);
        String parentTableName = AnnotationManager.getEntityTableName(objClass);
        String relationTableName = AnnotationManager.getEntityTableName(relationClass);
        Field fkFieldParentRelation = AnnotationManager.getFKFieldForEntity(objClass, relationClass);
        String fkRelationToParentColumnName = fkFieldParentRelation.getAnnotation(Column.class).name();
        String fkParentColumnName = fkFieldParentRelation.getAnnotation(ForeignKey.class).parentKey();

        return new NamedQuery("Select", String.format("Select r.* from %s r inner join %s p on r.%s = p.%s where p.%s = %s",
                relationTableName, // relation table
                parentTableName, // parent table
                fkRelationToParentColumnName, // relation to child foreign key column
                fkParentColumnName, // child foreign key column
                parentIdFieldName, // parent id column name
                String.valueOf(obj.getEntityId()) // parent id value
                ));
    }

    private NamedQuery getQueryInsert(Class<? extends EntityObject> c) {
        // get table name
        String tableName = getTableName(c);

        // init insert queryformat
        String insertQueryFormat = "INSERT INTO %s (%s) VALUES (%s)";

        // init column and parameter strings
        String columnCollectionString = "";
        String parameterCollectionString = "";

        // get column and parameter strings
        for (Entry<String, String> entry : AnnotationManager.GetEntityColumnsWithParameters(c, false).entrySet()) {
            String column = entry.getKey();
            String parameter = entry.getValue();

            columnCollectionString = columnCollectionString.concat(column).concat(",");
            parameterCollectionString = parameterCollectionString.concat("'").concat(parameter).concat("'").concat(",");
        }

        // delete "," in the end
        columnCollectionString = columnCollectionString.substring(0, columnCollectionString.length() - 1);
        parameterCollectionString = parameterCollectionString.substring(0, parameterCollectionString.length() - 1);

        return new NamedQuery(getQueryName(c, INamedQuery.NAMED_QUERY_ADD),
                String.format(insertQueryFormat, tableName, columnCollectionString, parameterCollectionString));
    }

    private NamedQuery getQueryUpdate(Class<? extends EntityObject> c) {
        // get table name
        String tableName = getTableName(c);

        // init insert queryformat
        String insertQueryFormat = "UPDATE %s SET %s WHERE %s";

        // init column and parameter strings
        String updateCollectionString = "";
        String parameterCollectionString = "";
        String entityIdColumn = AnnotationManager.GetEntityIdFieldName(c);

        // get column and parameter strings
        for (Entry<String, String> entry : AnnotationManager.GetEntityColumnsWithParameters(c, true).entrySet()) {
            String column = entry.getKey();
            String parameter = entry.getValue();

            if (!column.equals(entityIdColumn)) {
                updateCollectionString = updateCollectionString.concat(String.format("%s = '%s',", column, parameter));
            } else {
                parameterCollectionString = String.format("%s = '%s'", column, parameter);
            }
        }

        // delete "," in the end
        updateCollectionString = updateCollectionString.substring(0, updateCollectionString.length() - 1);

        return new NamedQuery(getQueryName(c, INamedQuery.NAMED_QUERY_UPDATE),
                String.format(insertQueryFormat, tableName, updateCollectionString, parameterCollectionString));
    }

    private NamedQuery getQueryDelete(Class<? extends EntityObject> c) {
        // get table name
        String tableName = getTableName(c);

        // get id field
        String fieldIdName = AnnotationManager.GetEntityIdFieldName(c);

        // init delete queryformat
        String deleteQueryFormat = "DELETE FROM %s WHERE %s = :%s";

        return new NamedQuery(getQueryName(c, INamedQuery.NAMED_QUERY_DELETE),
                String.format(deleteQueryFormat, tableName, fieldIdName, fieldIdName));
    }

    private NamedQuery getQueryUnique(Class<? extends EntityObject> c) {
        // get table name
        String tableName = getTableName(c);

        // get unique fields
        List<Field> uniqueFields = AnnotationManager.getFieldsByAnnotation(c, Unique.class);
        
        // if empty get all columns
        if (uniqueFields.isEmpty()){
            uniqueFields = AnnotationManager.getFieldsByAnnotation(c, Column.class);
        }

        // init mask
        String uniqueQueryFormat = "SELECT * FROM %s WHERE %s";

        // init column and parameter strings
        String collectionString = "";

        Map<String, String> columnsWithParameters = AnnotationManager.GetEntityColumnsWithParameters(c, false);
        // list unique queryformat
        for (Field field : uniqueFields) {
            if (!field.isAnnotationPresent(Column.class)) {
                try {
                    throw new UniqueException(Column.class.getName(), field.getName());
                } catch (UniqueException ex) {
                    Logger.getLogger(NamedQueryManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            String column = field.getAnnotation(Column.class).name();
            String parameter = columnsWithParameters.get(column);

            collectionString = collectionString.concat(column).concat("='").concat(parameter).concat("' and ");
        }

        // delete "," in the end
        collectionString = collectionString.substring(0, collectionString.length() - 5);

        return new NamedQuery(getQueryName(c, INamedQuery.NAMED_QUERY_UNIQUE),
                String.format(uniqueQueryFormat, tableName, collectionString));
    }

    private String getTableName(Class<? extends EntityObject> c) {
        return c.getAnnotation(Table.class).name();
    }
}
