package com.vetclinic;

import org.springframework.boot.SpringApplication;

public class TestVetclinicApplication {

	public static void main(String[] args) {
		SpringApplication.from(VetclinicApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
