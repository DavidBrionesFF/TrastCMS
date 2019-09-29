package com.bytecode.tratcms.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private HttpStatus status;
    private String message;
    private String method;
    private List<ErrorResponseSub> subErrors;

    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<ErrorResponseSub> getSubErrors() {
        return subErrors;
    }

    public void setSubErrors(List<ErrorResponseSub> subErrors) {
        this.subErrors = subErrors;
    }
}
