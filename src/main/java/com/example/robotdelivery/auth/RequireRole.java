package com.example.robotdelivery.auth;

import java.lang.annotation.*;

/**
 * 标注在 Controller 方法或类上，限制只有指定角色可访问。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    Role[] value();
}
