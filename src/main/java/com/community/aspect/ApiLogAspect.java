package com.community.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * API 日志与性能监控切面
 *
 * 功能：
 * 1. 记录每个 Controller 接口的请求信息（URI、方法、参数）
 * 2. 统计接口执行耗时
 * 3. 超过阈值（500ms）标记为慢接口，WARN 级别输出
 * 4. 异常捕获与错误日志
 *
 * 设计：@Around 环绕通知，无侵入式监控所有 Controller 方法
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ApiLogAspect {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 慢接口阈值（毫秒） */
    private static final long SLOW_THRESHOLD_MS = 500;

    /**
     * 切点：所有 Controller 包下的公共方法
     */
    @Pointcut("execution(public * com.community.controller..*.*(..))")
    public void controllerPointcut() {}

    @Around("controllerPointcut()")
    public Object aroundApi(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取请求信息
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        String method = joinPoint.getSignature().getName();
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String httpMethod = request != null ? request.getMethod() : "UNKNOWN";
        String uri = request != null ? request.getRequestURI() : "UNKNOWN";

        // 参数摘要（避免打印过长的 body）
        String args = truncateArgs(joinPoint);

        long startTime = System.currentTimeMillis();

        try {
            // 执行目标方法
            Object result = joinPoint.proceed();
            long costMs = System.currentTimeMillis() - startTime;

            // 根据耗时选择日志级别
            if (costMs > SLOW_THRESHOLD_MS) {
                log.warn("[慢接口] {}.{}() 耗时={}ms | {} {} | args=[{}]",
                        className, method, costMs, httpMethod, uri, args);
            } else {
                log.info("[API] {}.{}() 耗时={}ms | {} {} | args=[{}]",
                        className, method, costMs, httpMethod, uri, args);
            }

            return result;
        } catch (Throwable e) {
            long costMs = System.currentTimeMillis() - startTime;
            log.error("[异常] {}.{}() 耗时={}ms | {} {} | args=[{}] | error={}",
                    className, method, costMs, httpMethod, uri, args, e.getMessage());
            throw e;
        }
    }

    /**
     * 截断参数字符串，避免日志过长
     */
    private String truncateArgs(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature sig = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = sig.getParameterNames();
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) return "";

            Map<String, Object> paramMap = new HashMap<>();
            for (int i = 0; i < args.length; i++) {
                String name = (paramNames != null && i < paramNames.length) ? paramNames[i] : "arg" + i;
                Object val = args[i];
                // 过滤掉 RequestBody 等大对象，只保留基本类型
                if (val == null) {
                    paramMap.put(name, null);
                } else if (isSimpleType(val.getClass())) {
                    paramMap.put(name, val);
                } else {
                    paramMap.put(name, "[" + val.getClass().getSimpleName() + "]");
                }
            }
            String result = objectMapper.writeValueAsString(paramMap);
            return result.length() > 200 ? result.substring(0, 200) + "..." : result;
        } catch (Exception e) {
            return Arrays.toString(joinPoint.getArgs());
        }
    }

    private boolean isSimpleType(Class<?> type) {
        return type.isPrimitive() || type == String.class || type == Long.class
                || type == Integer.class || type == Boolean.class
                || type == Double.class || type == Float.class
                || type == java.math.BigDecimal.class;
    }
}
