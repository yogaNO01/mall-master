package com.macro.mall.portal.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.macro.mall.common.api.CommonResult;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.*;

@RestController
public class ExchangeRateController {
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final ObjectMapper mapper;
    private volatile Map<String,Object> cached;
    private volatile long refreshed;
    public ExchangeRateController(ObjectMapper mapper){this.mapper=mapper;}
    @GetMapping("/home/exchange-rates")
    public synchronized CommonResult<Map<String,Object>> rates(){
        if(cached!=null && System.currentTimeMillis()-refreshed<3600000) return CommonResult.success(cached);
        try {
            var request=HttpRequest.newBuilder(URI.create("https://api.frankfurter.dev/v2/rates?base=CNY&quotes=EUR,USD")).timeout(Duration.ofSeconds(12)).GET().build();
            var response=client.send(request,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200)throw new IllegalStateException("Rate provider unavailable");
            Map<String,BigDecimal> rates=new LinkedHashMap<>();rates.put("CNY",BigDecimal.ONE);
            String date="";
            for(JsonNode row:mapper.readTree(response.body())){String quote=row.path("quote").asText().toUpperCase(Locale.ROOT);BigDecimal rate=row.path("rate").decimalValue();if(rate.signum()>0){rates.put(quote,rate);date=row.path("date").asText();}}
            if(!rates.containsKey("EUR")||!rates.containsKey("USD"))throw new IllegalStateException("Missing rates");
            cached=Map.of("base","CNY","rates",rates,"date",date,"source","Frankfurter","frequency","daily");refreshed=System.currentTimeMillis();
            return CommonResult.success(cached);
        }catch(Exception e){
            if(e instanceof InterruptedException)Thread.currentThread().interrupt();
            if(cached!=null){Map<String,Object> stale=new LinkedHashMap<>(cached);stale.put("stale",true);return CommonResult.success(stale);}
            return CommonResult.failed("汇率暂时不可用");
        }
    }
}
