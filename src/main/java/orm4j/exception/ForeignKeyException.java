/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package orm4j.exception;

/**
 *
 * @author grig
 */
public class ForeignKeyException extends Exception {
    
    public ForeignKeyException(){
        super();
    }
    
    public ForeignKeyException(Class<?> mainClass, Class<?> relatedClass){
        super(String.format("Entity %s doesn't have foreign key for OneToMany Relation to %s",
                            relatedClass.getName(), mainClass.getName()));
        
    }
}
