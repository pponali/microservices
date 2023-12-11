package com.services.integrations.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setValue(String key, String value){
        redisTemplate.opsForValue().set(key, value);
    }

    public String getValue(String key){
        return redisTemplate.opsForValue().get(key);
    }

    public boolean hasKey(String key){
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public List<String> getRange(String key, int start, int end){

        return redisTemplate.opsForList().range(key, start, end);
    }

    public void deleteEntry(String key){
        redisTemplate.delete(key);
    }

    public void rightPushAll(String key){
        List<String> test = new ArrayList<>();
        test.add("1");
        test.add("2");
        test.add("3");
        test.add("4");
        redisTemplate.opsForList().rightPushAll(key, test);
        System.out.println(redisTemplate.opsForList().range(key, 0, -1));
    }

    public Long sizeOfList(String key){
        return redisTemplate.opsForList().size(key);
    }

    public void leftPush(String key, String value){
        redisTemplate.opsForList().leftPush(key, value);
        System.out.println("leftPush :: " + redisTemplate.opsForList().range(key, 0, -1));
    }

    public void leftPushAll(String key){
        List<String> test = new ArrayList<>();
        test.add("10");
        test.add("20");
        test.add("30");
        test.add("40");
        redisTemplate.opsForList().leftPushAll(key, test);
        System.out.println("leftPushAll :: " + redisTemplate.opsForList().range(key, 0, -1));

    }

    public void leftPushIfPresent(String key, String value){

        redisTemplate.opsForList().leftPushIfPresent(key, value);
        System.out.println("leftPushAll :: " + redisTemplate.opsForList().range(key, 0, -1));

    }

    public String valueAtIndex(String key, int index){
        return redisTemplate.opsForList().index(key, index);
    }

    public void trim(String key, int start, int end){
        redisTemplate.opsForList().trim(key, start, end);
        System.out.println("leftPushAll :: " + redisTemplate.opsForList().range(key, 0, -1));
    }


}
