/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package orm4j.annotation;

import java.lang.annotation.*;

/**
 *
 * @author grig
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {

    public enum ColumnType {

        STRING, INTEGER, DOUBLE, DATE
    }
    
    String name();
    ColumnType type() default ColumnType.STRING;
    String dateFormat() default "";
}
