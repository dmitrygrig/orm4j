/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package orm4j.exception;

/**
 *
 * @author grig
 */
public class DataProviderFetchingException extends Exception {

    public DataProviderFetchingException() {
        super();
    }

    public DataProviderFetchingException(Class<?> c) {
        super(String.format("Exception in DALController during fetching %s",
                c.getName()));

    }
}
