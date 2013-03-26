/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package orm4j;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import orm4j.annotation.AnnotationManager;
import orm4j.annotation.AutoIncremented;
import orm4j.annotation.Column;
import orm4j.annotation.EntityId;
import orm4j.annotation.EntityName;
import orm4j.annotation.ForeignKey;
import orm4j.annotation.Unique;
import orm4j.exception.ForeignKeyException;
import orm4j.query.NamedQueryParameter;
import orm4j.util.GlobalController;

/**
 *
 * @author grig
 */
public abstract class EntityObject implements IEntityObject {

    private Object getTypedObject(Object obj, Column col) throws ParseException {
        switch (col.type()) {
            case DATE:
                return obj.toString().equals("") ? new Date()
                        : col.dateFormat().isEmpty()
                        ? GlobalController.getDatetimeFormatter().parseObject(obj.toString())
                        : GlobalController.getDatetimeFormatter(col.dateFormat()).parseObject(obj.toString());
            case DOUBLE:
                return obj.toString().equals("") ? Double.MIN_VALUE : Double.parseDouble(obj.toString());
            case INTEGER:
                return obj.toString().equals("") ? Integer.MIN_VALUE : Integer.parseInt(obj.toString());
            default:
                return obj.toString();
        }
    }

    @Override
    public boolean setEntityId(Object id) {
        for (Field field : AnnotationManager.getFieldsByAnnotation(this.getClass(), EntityId.class)) {
            Column myAnnotation = field.getAnnotation(Column.class);

            if (myAnnotation == null) {
                return false;
            }

            return setColumnValue(id, myAnnotation.name());
        }

        return false;
    }

    @Override
    public Object getEntityId() {
        for (Field field : AnnotationManager.getFieldsByAnnotation(this.getClass(), EntityId.class)) {
            Column myAnnotation = field.getAnnotation(Column.class);

            if (myAnnotation == null) {
                return false;
            }

            return getColumnValue(myAnnotation.name());
        }

        return null;
    }

    @Override
    public boolean setEntityName(String name) {
        for (Field field : AnnotationManager.getFieldsByAnnotation(this.getClass(), EntityName.class)) {
            Column myAnnotation = field.getAnnotation(Column.class);

            if (myAnnotation == null) {
                return false;
            }

            return setColumnValue(name, myAnnotation.name());
        }

        return false;
    }

    @Override
    public String getEntityName() {
        for (Field field : AnnotationManager.getFieldsByAnnotation(this.getClass(), EntityName.class)) {
            Column myAnnotation = field.getAnnotation(Column.class);

            if (myAnnotation == null) {
                return null;
            }

            return (String) getColumnValue(myAnnotation.name());
        }

        return null;
    }

    /*
     * Sets value to variable, that has annotation Column and
     * Column.name() = columnName.
     * 
     */
    @Override
    public boolean setColumnValue(Object value, String columnName) {
        for (Field field : AnnotationManager.getFieldsByAnnotation(this.getClass(), Column.class)) {
            Column columnAnn = field.getAnnotation(Column.class);

            if (!columnAnn.name().equals(columnName)) {
                continue;
            }

            try {
                field.set(this, value);
                return true;
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(EntityObject.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(EntityObject.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return false;
    }

    /*
     * Gets value from variable, that has annotation Column and
     * Column.name() = columnName.
     * 
     */
    @Override
    public Object getColumnValue(String columnName) {
        for (Field field : AnnotationManager.getFieldsByAnnotation(this.getClass(), Column.class)) {
            Column columnAnn = field.getAnnotation(Column.class);

            if (!columnAnn.name().equals(columnName)) {
                continue;
            }
            try {
                return field.get(this);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(EntityObject.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(EntityObject.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return null;
    }

    @Override
    public void setValueForForeignKeyField(EntityObject mainObj) 
            throws ForeignKeyException {
        // get class of main obj
        Class mainClass = mainObj.getClass();
        
        // get fk Field corresponding to main class
        Field relEntityField = AnnotationManager.getFKFieldForEntity(mainClass, this.getClass());

        if (relEntityField == null) {
            throw new ForeignKeyException(mainClass, this.getClass());
        }


        // set value for corresponding variable
        this.setColumnValue(
                // get value from corresponding variable in main obj
                mainObj.getColumnValue(relEntityField.getAnnotation(ForeignKey.class).parentKey()),
                // get column name
                relEntityField.getAnnotation(Column.class).name());
    }

    @Override
    public void setObject(ResultSet resultSet) throws SQLException {
        for (Field field :
                AnnotationManager.getFieldsByAnnotation(
                this.getClass(),
                Column.class)) {
            Column myAnnotation = field.getAnnotation(Column.class);

            try {
                field.set(this, getTypedObject(resultSet.getString(myAnnotation.name()), myAnnotation));
            } catch (ParseException ex) {
                Logger.getLogger(EntityObject.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(EntityObject.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(EntityObject.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /*
     * hasAutoGenerated = true, isUnique = false.
     */
    public List<NamedQueryParameter> getNamedQueryParameters() {
        return this.getNamedQueryParameters(true, false);
    }

    /*
     * isUnique = false.
     */
    public List<NamedQueryParameter> getNamedQueryParameters(boolean hasAutoGenerated) {
        return this.getNamedQueryParameters(hasAutoGenerated, false);
    }

    @Override
    public List<NamedQueryParameter> getNamedQueryParameters(boolean hasAutoGenerated, boolean isUnique) {
        List<NamedQueryParameter> parameters = new ArrayList<NamedQueryParameter>();

        for (Field field : Arrays.asList(this.getClass().getDeclaredFields())) {
            if (isUnique && !field.isAnnotationPresent(Unique.class)) {
                continue;
            }

            if (!hasAutoGenerated && field.isAnnotationPresent(AutoIncremented.class)) {
                continue;
            }

            if (field.isAnnotationPresent(Column.class)) {
                Column myAnnotation = field.getAnnotation(Column.class);
                String value = null;
                // get value
                try {
                    Object valueObject = field.get(this);

                    if (valueObject != null) {
                        if (myAnnotation.type() == Column.ColumnType.DATE) {
                            value = myAnnotation.dateFormat().isEmpty()
                                    ? GlobalController.getDatetimeFormatter().format(valueObject)
                                    : GlobalController.getDatetimeFormatter(myAnnotation.dateFormat()).format(valueObject);
                        } else {
                            value = valueObject.toString();
                        }
                    }
                } catch (Exception ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                }
                parameters.add(new NamedQueryParameter(myAnnotation.name(), value));
            }
        }
        return parameters;
    }

    @Override
    public String toString() {
        String res = "\r\nDBObject: class = [" + this.getClass().getName() + "]\r\n\t{\r\n";

        for (NamedQueryParameter nqp : getNamedQueryParameters()) {
            res = res.concat(String.format("\t\t%s = [%s]\r\n", nqp.getKey(), nqp.getValue()));
        }
        res = res.concat("\t}\r\n");
        return res;
    }
}
