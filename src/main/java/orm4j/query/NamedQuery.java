/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package orm4j.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author grig
 */
public class NamedQuery implements INamedQuery {

    private String name;
    private String sqlString;
    private List<NamedQueryParameter> parameters = new ArrayList<NamedQueryParameter>();
    
    public NamedQuery(String sqlString) {
        this.name = "";
        this.sqlString = sqlString;
    }
    
    public NamedQuery(String name, String sqlString) {
        this.name = name;
        this.sqlString = sqlString;
    }

    public NamedQuery(String name, String sqlString, NamedQueryParameter... parameters) {
        this.name = name;
        this.sqlString = sqlString;
        this.parameters.addAll(Arrays.asList(parameters));
    }

    public NamedQuery(NamedQuery query) {
        this.name = query.getName();
        this.sqlString = query.sqlString;
    }

    public String getName() {
        return name;
    }

    public String getSqlString() {
        return sqlString;
    }

    public List getParameters() {
        return parameters;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSqlString(String sqlString) {
        this.sqlString = sqlString;
    }

    public void setParameters(NamedQueryParameter... parameters) {
        this.parameters.addAll(Arrays.asList(parameters));
    }

    public void setParameters(List parameters) {
        this.parameters = parameters;
    }

    @Override
    public String getFinalString() {
        String res = this.getSqlString();

        for (NamedQueryParameter nqp : this.parameters) {
            String key = nqp.getKey();
            key = "'" + key + "'";
            String value = nqp.getValue();
            if (!key.equalsIgnoreCase("':&limit'")) {
                res = res.replace(key,
                        value == null ? "''" : "'" + clearString(value) + "'");
            } else {
                res = res.concat(String.format(" Limit %s", value));
            }
        }

        return res;
    }

    private String clearString(String str) {
        str = str.replace("'", " ");
        return str;
    }
}
