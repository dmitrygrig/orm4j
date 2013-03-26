/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package orm4j.exception;

/**
 *
 * @author grig
 */
public class DataProviderAddingException extends Exception {
    public DataProviderAddingException(){
        super();
    }
    
    public DataProviderAddingException(Class<?> relatedClass){
        super(String.format("Exception in DALController during adding %s",
                                        relatedClass.getName()));
        
    }
}
