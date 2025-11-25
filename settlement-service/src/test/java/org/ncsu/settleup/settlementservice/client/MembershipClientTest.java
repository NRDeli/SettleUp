package org.ncsu.settleup.settlementservice.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MembershipClient}.  These tests mock the underlying
 * {@link RestTemplate} so that no HTTP calls are made.  Each test sets the
 * membershipServiceUrl via reflection to avoid a null pointer when constructing
 * request URLs.  Every branch of {@link MembershipClient#groupExists(Long)} and
 * {@link MembershipClient#memberExists(Long, Long)} is exercised to achieve
 * complete coverage of this class.
 */
class MembershipClientTest {

    private RestTemplate rest;
    private MembershipClient client;

    @BeforeEach
    void setUp() throws Exception {
        rest = mock(RestTemplate.class);
        client = new MembershipClient(rest);
        // Inject a base URL so that URL concatenation in the methods does not throw
        Field f = MembershipClient.class.getDeclaredField("membershipServiceUrl");
        f.setAccessible(true);
        f.set(client, "http://dummy");
    }

    @Test
    void groupExists_returnsTrue_whenRestCallSucceeds() {
        when(rest.getForEntity(anyString(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok().build());

        assertTrue(client.groupExists(1L), "Expected true when REST call does not throw");

        verify(rest, times(1))
                .getForEntity(contains("/groups/1"), eq(Object.class));
    }

    @Test
    void groupExists_returnsFalse_whenRestCallThrows() {
        when(rest.getForEntity(anyString(), eq(Object.class)))
                .thenThrow(new RuntimeException("fail"));

        assertFalse(client.groupExists(2L), "Expected false when REST call throws");

        verify(rest, times(1))
                .getForEntity(contains("/groups/2"), eq(Object.class));
    }

    @Test
    void memberExists_returnsFalse_whenRestCallThrows() {
        when(rest.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<List<Map<String, Object>>>>any()
        )).thenThrow(new RuntimeException("fail"));

        assertFalse(client.memberExists(1L, 2L), "Expected false when REST call throws");

        verify(rest, times(1)).exchange(
                contains("/groups/1/members"),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<List<Map<String, Object>>>>any()
        );
    }

    @Test
    void memberExists_returnsFalse_whenBodyNull() {
        ResponseEntity<List<Map<String, Object>>> response =
                new ResponseEntity<>(null, HttpStatus.OK);

        when(rest.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<List<Map<String, Object>>>>any()
        )).thenReturn(response);

        assertFalse(client.memberExists(1L, 3L), "Expected false when response body is null");

        verify(rest, times(1)).exchange(
            contains("/groups/1/members"),
            eq(HttpMethod.GET),
            isNull(),
            ArgumentMatchers.<ParameterizedTypeReference<List<Map<String, Object>>>>any()
        );
    }

    @Test
    void memberExists_returnsFalse_whenIdNotFound() {
        // Response body contains a member but not the searched ID
        List<Map<String, Object>> members = new ArrayList<>();
        Map<String, Object> m = new HashMap<>();
        m.put("id", 99L);
        members.add(m);

        ResponseEntity<List<Map<String, Object>>> response =
                new ResponseEntity<>(members, HttpStatus.OK);

        when(rest.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<List<Map<String, Object>>>>any()
        )).thenReturn(response);

        assertFalse(client.memberExists(2L, 1L), "Expected false when memberId not in list");

        verify(rest, times(1)).exchange(
                contains("/groups/2/members"),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<List<Map<String, Object>>>>any()
        );
    }

    @Test
    void memberExists_returnsTrue_whenIdFound() {
        List<Map<String, Object>> members = new ArrayList<>();
        Map<String, Object> m1 = new HashMap<>();
        m1.put("id", 5L);
        members.add(m1);

        ResponseEntity<List<Map<String, Object>>> response =
                new ResponseEntity<>(members, HttpStatus.OK);

        when(rest.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<List<Map<String, Object>>>>any()
        )).thenReturn(response);

        assertTrue(client.memberExists(3L, 5L),
                "Expected true when the id is present in response body");

        verify(rest, times(1)).exchange(
                contains("/groups/3/members"),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<List<Map<String, Object>>>>any()
        );
    }
}