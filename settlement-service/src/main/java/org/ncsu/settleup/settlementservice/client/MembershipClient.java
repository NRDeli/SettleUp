package org.ncsu.settleup.settlementservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Client for interacting with the membership service.  It is used by the
 * settlement service to verify the existence of groups and members
 * before recording or updating transfers.
 */
@Service
public class MembershipClient {
    @Value("${membership.service.url:http://localhost:8081}")
    private String membershipServiceUrl;
    private final RestTemplate restTemplate;

    public MembershipClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean groupExists(Long groupId) {
        try {
            restTemplate.getForEntity(membershipServiceUrl + "/groups/" + groupId, Object.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean memberExists(Long groupId, Long memberId) {
        try {
            ResponseEntity<List> response = restTemplate.getForEntity(
                    membershipServiceUrl + "/groups/" + groupId + "/members",
                    List.class);
            List<?> members = response.getBody();
            if (members == null) {
                return false;
            }
            for (Object obj : members) {
                if (obj instanceof Map<?, ?> map) {
                    Object idObj = map.get("id");
                    if (idObj instanceof Number num && num.longValue() == memberId) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}