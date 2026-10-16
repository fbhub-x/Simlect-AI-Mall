package com.simlect.biz.impl;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.simlect.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AliEmailServiceImplTest {
    private AliEmailServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AliEmailServiceImpl();
        ReflectionTestUtils.setField(service, "accessKeyId", "test-access-key-id");
        ReflectionTestUtils.setField(service, "accessKeySecret", "test-access-key-secret");
        ReflectionTestUtils.setField(service, "accountName", "notice@example.com");
        ReflectionTestUtils.setField(service, "replyToAddress", "false");
    }

    static Stream<Arguments> missingConfiguration() {
        return Stream.of("accessKeyId", "accessKeySecret", "accountName")
                .flatMap(field -> Stream.of(null, "", "   ").map(value -> Arguments.of(field, value)));
    }

    @ParameterizedTest
    @MethodSource("missingConfiguration")
    void missingConfiguration_rejectsBeforeCreatingCloudClient(String field, String value) {
        ReflectionTestUtils.setField(service, field, value);
        try (var clients = mockConstruction(DefaultAcsClient.class)) {
            BusinessException error = assertThrows(BusinessException.class,
                    () -> service.sendVerificationCode("recipient@example.com"));

            assertEquals("邮件发送服务未配置，请联系管理员", error.getMessage());
            assertTrue(clients.constructed().isEmpty());
        }
    }

    @Test
    void successfulSend_returnsSixDigitCode() throws Exception {
        CommonResponse response = new CommonResponse();
        response.setData("{\"RequestId\":\"test-request-id\"}");
        try (var clients = mockConstruction(DefaultAcsClient.class,
                (client, context) -> when(client.getCommonResponse(any(CommonRequest.class))).thenReturn(response))) {
            String code = service.sendVerificationCode("recipient@example.com");

            assertTrue(code.matches("[0-9]{6}"));
            verify(clients.constructed().get(0)).getCommonResponse(any(CommonRequest.class));
        }
    }

    static Stream<ClientException> cloudFailures() {
        return Stream.of(new ClientException("InvalidAccessKeyId", "provider details"),
                new ServerException("ServiceUnavailable", "provider details"));
    }

    @ParameterizedTest
    @MethodSource("cloudFailures")
    void cloudFailure_returnsBusinessErrorWithoutExposingProviderDetails(ClientException failure) {
        try (var clients = mockConstruction(DefaultAcsClient.class,
                (client, context) -> when(client.getCommonResponse(any(CommonRequest.class))).thenThrow(failure))) {
            BusinessException error = assertThrows(BusinessException.class,
                    () -> service.sendVerificationCode("recipient@example.com"));

            assertEquals("验证码邮件发送失败，请稍后重试", error.getMessage());
            assertSame(failure, error.getCause());
        }
    }
}
