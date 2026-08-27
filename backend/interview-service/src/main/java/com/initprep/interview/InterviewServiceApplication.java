package com.initprep.interview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class InterviewServiceApplication {

	public static void main(String[] args) {
        //System.out.println(new BCryptPasswordEncoder().encode("@LalitPass12"));
		SpringApplication.run(InterviewServiceApplication.class, args);
	}

}
