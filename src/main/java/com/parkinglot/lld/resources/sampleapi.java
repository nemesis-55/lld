package com.parkinglot.lld.resources;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Health API", description = "Health check endpoints")

public class sampleapi {

    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Returns application health status")
    public String health() {
        return "Application is running";
    }

    @PostMapping("/sample")
    public String samplePost(String testing) {
        return testing;   
    }


}
