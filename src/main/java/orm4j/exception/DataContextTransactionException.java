/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package orm4j.exception;

/**
 *
 * @author grig
 */
public class DataContextTransactionException extends Exception {
     public DataContextTransactionException(){
        super();
    }

    public DataContextTransactionException(String message) {
        super(message);
    }
}
