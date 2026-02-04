package com.parkinglot.lld;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI parkingLotOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Parking Lot LLD APIs")
                        .description("REST APIs for Parking Lot System")
                        .version("1.0"));
    }
}
