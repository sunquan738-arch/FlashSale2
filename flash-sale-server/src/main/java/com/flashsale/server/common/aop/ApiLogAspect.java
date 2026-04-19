package com.flashsale.server.common.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ApiLogAspect {

    private static final Pattern SENSITIVE_PATTERN = Pattern.compile("(?i)(\\\"(?:password|token|authorization|phone)\\\"\\s*:\\s*\\\")(.*?)(\\\")");

    private final ObjectMapper objectMapper;

    @Around("execution(* com.flashsale.server.interfaces.rest..*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();
        String uri = resolveUri();
        String args = serializeArgs(joinPoint.getArgs());

        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;
            log.info("API OK | uri={} | method={} | cost={}ms | args={} | result={}",
                    uri, methodName, cost, args, safeJson(result));
            return result;
        } catch (Throwable ex) {
            long cost = System.currentTimeMillis() - start;
            log.error("API ERR | uri={} | method={} | cost={}ms | args={} | ex={}",
                    uri, methodName, cost, args, ex.getMessage());
            throw ex;
        }
    }

    private String resolveUri() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "N/A";
        }
        HttpServletRequest request = attributes.getRequest();
        return request.getMethod() + " " + request.getRequestURI();
    }

    private String serializeArgs(Object[] args) {
        return Arrays.stream(args)
                .filter(arg -> !(arg instanceof HttpServletRequest))
                .filter(arg -> !(arg instanceof HttpServletResponse))
                .map(this::safeJson)
                .collect(Collectors.joining(","));
    }

    private String safeJson(Object value) {
        if (value == null) {
            return "null";
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            String masked = SENSITIVE_PATTERN.matcher(json).replaceAll("$1***$3");
            return masked.length() > 2000 ? masked.substring(0, 2000) + "...(truncated)" : masked;
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }
}
