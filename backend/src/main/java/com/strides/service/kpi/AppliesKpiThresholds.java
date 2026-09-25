package com.strides.service.kpi;

import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** On a KPI service class: every List&lt;KpiCardResponse&gt; it returns gets threshold status stamped on - see KpiThresholdStatusInterceptor. */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AppliesKpiThresholds {
}
