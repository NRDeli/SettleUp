package org.ncsu.settleup.expenseservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Client for interacting with the membership service.  Provides simple
 * methods to check for the existence of groups and members.  This
 * enables the expense service to validate cross-service invariants
 * before persisting data.
 */
@Service
public class MembershipClient {
    /** Base URL of the membership service (e.g. http://localhost:8081). */
    @Value("${membership.service.url:http://localhost:8081}")
    private String membershipServiceUrl;

    private final RestTemplate restTemplate;

    public MembershipClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Check whether a group with the given ID exists by calling the
     * membership service.  If the membership service responds with a
     * non-200 code (e.g. 404), this method returns false.
     *
     * @param groupId group identifier
     * @return true if the group exists; false otherwise
     */
    public boolean groupExists(Long groupId) {
        try {
            restTemplate.getForEntity(membershipServiceUrl + "/groups/" + groupId, Object.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check whether a given member exists in the specified group.
     *
     * @param groupId  group identifier
     * @param memberId member identifier
     * @return true if the member exists and belongs to the group
     */
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