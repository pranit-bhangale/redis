package com.pranit.inventory.aop.aspect;

import com.pranit.inventory.aop.annotation.RateLimit;
import com.pranit.inventory.aop.annotation.RateLimits;
import com.pranit.inventory.exception.RateLimitExceededException;
import com.pranit.inventory.model.RateLimitPolicy;
import com.pranit.inventory.model.RateLimitResult;
import com.pranit.inventory.service.ClientIdentity;
import com.pranit.inventory.service.RateLimitMetrics;
import com.pranit.inventory.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);
    private final RateLimitService service;
    private final ClientIdentity identity;
    private final RateLimitMetrics metrics;
    private final ParameterNameDiscoverer paramNames = new DefaultParameterNameDiscoverer();
    private final ExpressionParser parser = new SpelExpressionParser();
    private final Map<Method, List<Compiled>> cache = new ConcurrentHashMap<>();

    public RateLimitAspect(RateLimitService service, ClientIdentity identity, RateLimitMetrics metrics) {
        this.service = service;
        this.identity = identity;
        this.metrics = metrics;
    }

    // One @RateLimit is present directly; two or more are wrapped by the @RateLimits container.
    @Around("@annotation(com.pranit.inventory.aop.annotation.RateLimit) "
            + "|| @annotation(com.pranit.inventory.aop.annotation.RateLimits)")
    public Object enforce(ProceedingJoinPoint pjp) throws Throwable {
        // Only applies to HTTP requests (skips schedulers, async jobs, plain unit tests)
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return pjp.proceed();
        }
        HttpServletRequest request = attrs.getRequest();
        HttpServletResponse response = attrs.getResponse();
        Method declared = ((MethodSignature) pjp.getSignature()).getMethod();
        Method method = AopUtils.getMostSpecificMethod(declared, pjp.getTarget().getClass());
        List<Compiled> policies = cache.computeIfAbsent(method, this::compile);
        long minRemaining = Long.MAX_VALUE;
        int minLimit = 0;
        // All stacked policies must pass
        for (Compiled c : policies) {
            Key key = resolveKey(c, request, pjp, method);
            RateLimitResult r = service.check(c.policy(), key.scope(), key.id());
            if (!r.allowed()) {
                long retrySeconds = Math.max(1, Math.ceilDiv(r.retryAfterMs(), 1000L));
                throw new RateLimitExceededException(retrySeconds);
            }
            if (r.remaining() >= 0 && r.remaining() < minRemaining) {
                minRemaining = r.remaining();
                minLimit = c.policy().limit();
            }
        }

        if (response != null && minRemaining != Long.MAX_VALUE) {
            response.setIntHeader("X-RateLimit-Limit", minLimit);
            response.setHeader("X-RateLimit-Remaining", Long.toString(minRemaining));
        }
        return pjp.proceed();
    }

    /**
     * Runs once per method: read annotations, validate, pre-parse SpEL.
     */
    private List<Compiled> compile(Method method) {
        Set<RateLimit> annotations = AnnotatedElementUtils
                .getMergedRepeatableAnnotations(method, RateLimit.class, RateLimits.class);
        List<Compiled> out = new ArrayList<>(annotations.size());
        int index = 0;
        for (RateLimit a : annotations) {
            String defaultName = method.getDeclaringClass().getSimpleName()
                    + "#" + method.getName() + "#" + (index++);
            RateLimitPolicy policy = RateLimitPolicy.from(a, defaultName);
            Expression expr = a.key() == RateLimit.KeyType.EXPRESSION
                    ? parser.parseExpression(a.keyExpression()) : null;
            out.add(new Compiled(policy, expr));
        }
        return List.copyOf(out);
    }

    private Key resolveKey(Compiled c, HttpServletRequest req, ProceedingJoinPoint pjp, Method method) {
        String ip = identity.ip(req);
        return switch (c.policy().key()) {
            case IP -> new Key("ip", ip);
            case USER -> {
                String user = identity.user(req);
                if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
                yield new Key("u", ClientIdentity.hash(user));
            }
            case USER_OR_IP -> {
                String user = identity.user(req);
                yield user != null ? new Key("u", ClientIdentity.hash(user)) : new Key("ip", ip);
            }
            case EXPRESSION -> {
                String value = evaluate(c, pjp, method);
                if (value == null || value.isBlank()) {
                    yield new Key("ip", ip);
                }
                yield new Key("x", ClientIdentity.hash(value));
            }
        };
    }

    private String evaluate(Compiled c, ProceedingJoinPoint pjp, Method method) {
        try {
            var ctx = new MethodBasedEvaluationContext(
                    pjp.getTarget(), method, pjp.getArgs(), paramNames);
            Object v = c.keyExpr().getValue(ctx);
            if (v == null || v.toString().isBlank()) {
                metrics.keyFallback(c.policy().name(), "blank");
                return null;
            }
            return v.toString();
        } catch (Exception ex) {
            metrics.keyFallback(c.policy().name(), "error");
            log.warn("keyExpression failed on {}: {}", method, ex.toString());
            return null;
        }
    }

    /**
     * A policy plus its pre-parsed SpEL expression (null unless key=EXPRESSION).
     */
    private record Compiled(RateLimitPolicy policy, Expression keyExpr) {
    }

    private record Key(String scope, String id) {
    }
}