package org.ncsu.settleup.membershipservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the membership service.  This service manages groups,
 * members and categories.  It exposes a REST API described by the
 * MembershipController.
 */
@SpringBootApplication
public class MembershipServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MembershipServiceApplication.class, args);
    }
}
