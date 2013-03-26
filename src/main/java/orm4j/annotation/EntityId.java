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
public @interface EntityId {
    
}
