/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package orm4j.query;

/**
 *
 * @author grig
 */
public interface INamedQuery {
    public static final String NAMED_QUERY_ADD = "add";
    public static final String NAMED_QUERY_DELETE = "delete";
    public static final String NAMED_QUERY_UPDATE = "update";
    public static final String NAMED_QUERY_GETALL = "findAll";
    public static final String NAMED_QUERY_GETBYID = "findById";
    public static final String NAMED_QUERY_GETBYNAME = "findByName";
    public static final String NAMED_QUERY_COUNT = "findCount";
    public static final String NAMED_QUERY_UNIQUE = "unique";
    public static final String NAMED_QUERY_TRANSACTION = "transaction";
    public static final String NAMED_QUERY_COMMIT = "commit";
    public static final String NAMED_QUERY_ROLLBACK = "rollback";
    
    public String getFinalString();
}
