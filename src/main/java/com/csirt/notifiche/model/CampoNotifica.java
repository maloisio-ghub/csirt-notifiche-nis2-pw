package com.csirt.notifiche.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Dichiara un campo dei documenti di notifica e, per ogni adempimento, se è obbligatorio o facoltativo.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CampoNotifica {

    int ordine();

    String etichetta();

    TipoDocumento[] obbligatorioIn() default {};

    TipoDocumento[] facoltativoIn() default {};

    String riferimento() default "";
}