package com.orange.oss.osbcmdb;

import reactor.core.publisher.Hooks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OsbCmdbApplication {

    public static void main(String[] args) {
		Hooks.onOperatorDebug(); //Turn on debugging
    	SpringApplication.run(OsbCmdbApplication.class, args);
    }



}
