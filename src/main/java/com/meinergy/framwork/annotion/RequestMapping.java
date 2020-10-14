package com.meinergy.framwork.annotion;

import java.lang.annotation.*;

/**
 * RequestMapping
 *
 * @author chenwang
 * @date 2020/10/13
 */
@Documented
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMapping {
    String value() default "";
}
