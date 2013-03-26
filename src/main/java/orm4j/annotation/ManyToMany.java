/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package orm4j.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import orm4j.EntityObject;

/**
 *
 * @author grig
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ManyToMany {
    Class<? extends EntityObject>[] relatedTo();
    Class<? extends EntityObject> relatedThrough();
    Class<?> collection() default List.class;
}
