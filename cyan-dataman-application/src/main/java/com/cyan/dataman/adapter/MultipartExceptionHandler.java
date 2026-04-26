package com.cyan.dataman.adapter;

import com.cyan.arch.common.api.ErrorCode;
import com.cyan.arch.common.api.Response;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class MultipartExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Response<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        return new Response<>(ErrorCode.FAILED.getCode(), "上传文件大小超出限制", null, null);
    }
}
