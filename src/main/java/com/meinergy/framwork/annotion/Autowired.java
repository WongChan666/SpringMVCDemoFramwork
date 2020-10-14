package com.meinergy.framwork.annotion;

import java.lang.annotation.*;

/**
 * Autowired
 *
 * @author chenwang
 * @date 2020/10/13
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Autowired {
    String value() default "";
}
