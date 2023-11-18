package com.services.integrations.controller;


import com.services.integrations.service.RedisService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value ="/api/v1")
public class RedisController {

    private final RedisService redisService;

    public RedisController(RedisService redisService) {
        this.redisService = redisService;
    }

    @GetMapping(value = "/get/{id}")
    public String key(@PathVariable String id){
        return redisService.getValue(id);
    }

    @PutMapping(value = "/put")
    public void upsertValue(@RequestParam String key, @RequestParam String value){
        redisService.setValue(key, value);
    }
}
