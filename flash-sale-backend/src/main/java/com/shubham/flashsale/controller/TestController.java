package com.shubham.flashsale.controller;

import com.shubham.flashsale.service.TestResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")  //aloow frontend to call this endpoint
public class TestController {

    @Autowired
    private TestResetService testResetService;

    @PostMapping("/reset-all")
    public ResponseEntity<String> resetAllForTest(){
        testResetService.resetEntireSystem();
        return ResponseEntity.ok("Global reset successful! All orders wiped and allproducts restored in MySQL and Redis.");
    }
}
