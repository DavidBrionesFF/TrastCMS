package com.bytecode.tratcms.model.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RepBase<T> {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date timestamp = new Date();

    private T response;

    private long count;

    public RepBase() {
    }

    public RepBase(T response) {
        this.response = response;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public T getResponse() {
        return response;
    }

    public void setResponse(T response) {
        this.response = response;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public RepBase addCount(long count){
        this.count = count;
        return this;
    }

    public RepBase addResponse(T response){
        this.response = response;
        return this;
    }

    public static RepBase create(){
        return new RepBase();
    }

    public static RepBase create(long count, Object response){
        RepBase repBase = new RepBase();
        repBase.addCount(count);
        repBase.addResponse(response);
        return repBase;
    }
}
