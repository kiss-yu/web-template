package com.keray.common.config;

import com.keray.common.CommonResultCode;
import com.keray.common.Result;
import com.keray.common.exception.CodeException;
import com.keray.common.utils.QpsLimit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

/**
 * @author by keray
 * date:2020/9/7 9:36 下午
 */
@Slf4j
@Configuration
public class ResultServletInvocableHandlerMethodHandler implements ServletInvocableHandlerMethodHandler, ExceptionHandler<Throwable> {

    private final static ExceptionHandler[] EXCEPTION_HANDLERS = new ExceptionHandler[]{
            new CodeExceptionHandler(),
            new QpsExceptionHandler()
    };

    @Override
    public Integer order() {
        return 1;
    }

    @Override
    public Object work(HandlerMethod handlerMethod, Object[] args, NativeWebRequest request, ServletInvocableHandlerMethodCallback callback) throws Exception {
        try {
            Object result = callback.get();
            if (result instanceof Result) {
                return result;
            }
            return Result.success(result);
        } catch (Throwable error) {
            return errorHandler(error);
        }
    }


    @Override
    public boolean supper(Throwable e) {
        return true;
    }

    @Override
    public Result<?> errorHandler(Throwable error) {
        for (ExceptionHandler<Throwable> eh : EXCEPTION_HANDLERS) {
            if (eh.supper(error)) {
                return eh.errorHandler(error);
            }
        }
        return Result.fail(CommonResultCode.unknown);
    }
}

interface ExceptionHandler<E extends Throwable> {
    boolean supper(Throwable e);

    Result<?> errorHandler(E error);
}

/**
 * 具有code-message的错误处理
 */
class CodeExceptionHandler implements ExceptionHandler<Throwable> {

    @Override
    public boolean supper(Throwable e) {
        return e instanceof CodeException;
    }

    @Override
    public Result<?> errorHandler(Throwable error) {
        CodeException ex = (CodeException) error;
        return Result.fail(ex.getCode(), ex.getMessage(), error);
    }
}

class QpsExceptionHandler implements ExceptionHandler<QpsLimit.QPSFailException> {

    @Override
    public boolean supper(Throwable e) {
        return e instanceof QpsLimit.QPSFailException;
    }

    @Override
    public Result<?> errorHandler(QpsLimit.QPSFailException error) {
        return Result.fail(CommonResultCode.limitedAccess);
    }
}