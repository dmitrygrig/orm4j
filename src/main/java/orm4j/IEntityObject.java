/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package orm4j;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;
import orm4j.exception.ForeignKeyException;
import orm4j.query.NamedQueryParameter;

/**
 *
 * @author grig
 */
public interface IEntityObject {

    public void setObject(ResultSet resultSet) throws SQLException, ParseException;

    public List<NamedQueryParameter> getNamedQueryParameters(boolean hasAutoGenerated, boolean isUnique);

    public boolean setEntityId(Object id);

    public Object getEntityId();

    public boolean setEntityName(String name);

    public String getEntityName();

    public Object getColumnValue(String columnName);

    public boolean setColumnValue(Object value, String columnName);

    public void setValueForForeignKeyField(EntityObject mainObj) throws ForeignKeyException;
}