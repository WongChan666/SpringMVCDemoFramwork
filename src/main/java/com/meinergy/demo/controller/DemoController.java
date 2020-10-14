package com.meinergy.demo.controller;

import com.meinergy.demo.service.DemoService;
import com.meinergy.framwork.annotion.Autowired;
import com.meinergy.framwork.annotion.Controller;
import com.meinergy.framwork.annotion.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * DemoController
 *
 * @author chenwang
 * @date 2020/10/13
 */
@Controller
@RequestMapping("/demo")
public class DemoController {

    @Autowired
    private DemoService demoService;

    @RequestMapping("/query")
    public void query(HttpServletRequest request, HttpServletResponse response,String name) throws IOException {
        response.getWriter().write(demoService.get(name));
    }
}
