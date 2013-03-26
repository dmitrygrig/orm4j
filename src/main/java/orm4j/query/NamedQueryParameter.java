/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package orm4j.query;

/**
 *
 * @author grig
 */
public class NamedQueryParameter {

    private String key;
    private String value;

    public NamedQueryParameter(String key, String value) {
        this.key = String.format(":%s", key);
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public static NamedQueryParameter getLimit(int position, int offset) {
        return new NamedQueryParameter("&limit", String.format("%d,%d", position, offset));
    }
}
