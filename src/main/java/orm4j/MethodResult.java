/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package orm4j;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author grig
 */
public final class MethodResult {

    private int exception;
    private Map<String, Object> values;

    public MethodResult(){
        this.setException(IDataProvider.NO_EXCEPTION);
        values= new HashMap<String, Object>();
    }
    
    public MethodResult(int exception) {
        this.exception = exception;
    }

    public int getException() {
        return exception;
    }

    public void setException(int exception) {
        this.exception = exception;
    }

    public Map<String, Object> getValues() {
        return values;
    }
    
    public Object getValue(String key){
        return getValues().get(key);
    }

    public void setValues(Map<String, Object> outputParameter) {
        this.values = outputParameter;
    }
    
    public void addValue(String key, Object value){
        this.values.put(key, value);
    }
    
    public Boolean isSuccessful(){
        return this.getException() == IDataProvider.NO_EXCEPTION;
    }
}
