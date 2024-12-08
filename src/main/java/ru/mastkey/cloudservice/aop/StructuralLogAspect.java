package ru.mastkey.cloudservice.aop;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;
import java.util.UUID;

@Aspect
@Component
public class StructuralLogAspect {
    public static final String HEADER_REQUEST_ID = "X-Request-ID";
    private static final String MDC_REQUEST_ID = "RequestId";

    @Around("@within(ru.mastkey.cloudservice.aop.StructuralLogWithRequestIdFieldAnnotation) || " +
            "@annotation(ru.mastkey.cloudservice.aop.StructuralLogWithRequestIdFieldAnnotation)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        HttpServletRequest request = getCurrentHttpRequest();
        String requestId = (Objects.nonNull(request))
                ? request.getHeader(HEADER_REQUEST_ID)
                : null;

        requestId = StringUtils.hasText(requestId)
                ? requestId
                : UUID.randomUUID().toString();

        try (var ignored = MDC.putCloseable(MDC_REQUEST_ID, requestId)) {
            return pjp.proceed();
        }
    }

    private HttpServletRequest getCurrentHttpRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest();
    }
}