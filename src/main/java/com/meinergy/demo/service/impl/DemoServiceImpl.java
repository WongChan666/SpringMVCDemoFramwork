package com.meinergy.demo.service.impl;

import com.meinergy.demo.service.DemoService;
import com.meinergy.framwork.annotion.Service;

/**
 * DemoServiceImpl
 *
 * @author chenwang
 * @date 2020/10/13
 */
@Service
public class DemoServiceImpl implements DemoService {
    @Override
    public String get(String name) {
        System.out.println("service中的参数："+name);
        return name;
    }
}
