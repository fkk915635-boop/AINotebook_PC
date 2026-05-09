package com.ainotebook.note.controller;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(IllegalArgumentException e) {
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler(BadSqlGrammarException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleSql(BadSqlGrammarException e) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        if (msg.contains("notes") && msg.toLowerCase().contains("user_id")) {
            return new ErrorResponse("数据库表 notes 缺少 user_id 字段，请执行：ALTER TABLE notes ADD COLUMN user_id BIGINT NOT NULL DEFAULT 0; 并重新启动服务");
        }
        return new ErrorResponse("数据库错误：" + (msg.length() > 200 ? msg.substring(0, 200) : msg));
    }

    public static class ErrorResponse {
        private String message;

        public ErrorResponse() {
        }

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}

