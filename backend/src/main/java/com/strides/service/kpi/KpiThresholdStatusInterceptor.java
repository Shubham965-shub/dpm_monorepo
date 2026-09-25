package com.strides.service.kpi;

import com.strides.dto.KpiCardResponse;
import com.strides.service.KpiThresholdConfigService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.util.List;

/**
 * Stamps threshold status onto every KPI row in one place, instead of in each of the ~40 KPI
 * service methods. Anything that isn't a list of KpiCardResponse (e.g. filter-option maps)
 * passes through untouched.
 */
@AppliesKpiThresholds
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class KpiThresholdStatusInterceptor {

    @Inject
    KpiThresholdConfigService thresholdConfig;

    @AroundInvoke
    Object stampStatus(InvocationContext context) throws Exception {
        Object result = context.proceed();
        if (result instanceof List<?> rows && !rows.isEmpty() && rows.get(0) instanceof KpiCardResponse) {
            @SuppressWarnings("unchecked")
            List<KpiCardResponse> cards = (List<KpiCardResponse>) rows;
            return thresholdConfig.applyTo(cards);
        }
        return result;
    }
}
