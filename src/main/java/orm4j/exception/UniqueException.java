/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package orm4j.exception;

/**
 *
 * @author grig
 */
public class UniqueException extends Exception {

    public UniqueException() {
    }

    public UniqueException(String className, String fieldName) {
        super(String.format("Annotation has wrong notation for class % column %s.", className, fieldName));
    }
    
}
