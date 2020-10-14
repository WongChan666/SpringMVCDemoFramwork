package com.meinergy.framwork.annotion;

import java.lang.annotation.*;

/**
 * Service
 *
 * @author chenwang
 * @date 2020/10/13
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Service {
    String value() default "";
}
