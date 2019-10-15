package com.lgak.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StateEnum {

    LOGIN_SUCCESS(1,"登录成功"),

    LOGIN_TIME_OR_TOKEN_ERROR(-2,"登录超时或token错误"),

    PARAMETER_ERROR(-4,"参数错误"),

    USERNAME_OR_PASSWORD_ERROR(-3,"用户名或密码错误");

    private int state;

    private String msg;


}
