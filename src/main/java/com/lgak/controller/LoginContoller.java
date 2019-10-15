package com.lgak.controller;

import bean.SysUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgak.mapper.user.SysUserMapper;
import com.lgak.utils.StateEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Slf4j
@RestController
public class LoginContoller {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private RedissonClient redissonClient;

    @PostMapping("/login")
    public Object login(@RequestBody Map<String, String> user) throws JsonProcessingException {

        HashMap<String, Object> hashMap = new HashMap<>();
        String username = user.get("username");
        String password = user.get("password");
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)){
            hashMap.put("status", StateEnum.PARAMETER_ERROR.getState());
            hashMap.put("msg", StateEnum.PARAMETER_ERROR.getMsg());
            return hashMap;
        }
        String passwordForMd5 = DigestUtils.md5Hex(password);
        SysUser sysUser = SysUser.builder().loginName(username).password(passwordForMd5).build();
        List<SysUser> userList = sysUserMapper.select(sysUser);

        if (userList.size() != 0) {
            String token = DigestUtils.md5Hex(username + password + System.currentTimeMillis());
            hashMap.put("token", token);
            hashMap.put("username", username);
            hashMap.put("msg", StateEnum.LOGIN_SUCCESS.getMsg());
            hashMap.put("status", StateEnum.LOGIN_SUCCESS.getState());
            ObjectMapper mapper = new ObjectMapper();
            String sysUserResult = mapper.writeValueAsString(userList.get(0));
            RMap<Object, Object> map = redissonClient.getMap(username);
            map.fastPut("token", token);
            map.fastPut("detail", sysUserResult);
            map.expire(30, TimeUnit.MINUTES);
        } else {
            hashMap.put("status", StateEnum.USERNAME_OR_PASSWORD_ERROR.getState());
            hashMap.put("msg", StateEnum.USERNAME_OR_PASSWORD_ERROR.getMsg());
            return hashMap;
        }

        return hashMap;
    }
}
