package com.omni.recommender.behavior.application.service;

import com.omni.recommender.behavior.application.command.LogBehaviorCommand;
import com.omni.recommender.behavior.application.port.out.BehaviorLocalLoggerPort;
import com.omni.recommender.behavior.domain.behavior.aggregate.root.UserBehavior;
import com.omni.recommender.behavior.domain.behavior.aggregate.vo.BehaviorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

class BehaviorApplicationServiceTest {

    private BehaviorLocalLoggerPort loggerPort;
    private BehaviorApplicationService applicationService;

    @BeforeEach
    void setUp() {
        // Mock Outbound Port
        loggerPort = Mockito.mock(BehaviorLocalLoggerPort.class);
        // 初始化 Application Service
        applicationService = new BehaviorApplicationService(loggerPort);
    }

    @Test
    void shouldLogBehaviorSuccessfully() {
        // Arrange (準備 Command)
        LogBehaviorCommand command = new LogBehaviorCommand(
                "U1001",
                "S2002",
                "Item3003",
                "CLICK",
                "192.168.1.1",
                "Mozilla/5.0",
                "https://google.com",
                Map.of("position", "1"),
                Instant.now()
        );

        // Act (執行業務邏輯)
        applicationService.execute(command);

        // Assert (驗證結果與邊界行為)
        ArgumentCaptor<UserBehavior> captor = ArgumentCaptor.forClass(UserBehavior.class);
        verify(loggerPort).writeLog(captor.capture()); // 驗證 Outbound Port 是否正確被呼叫

        UserBehavior savedBehavior = captor.getValue();
        assertNotNull(savedBehavior.getBehaviorId());
        assertEquals("U1001", savedBehavior.getUserId().value());
        assertEquals("S2002", savedBehavior.getSessionId().value());
        assertEquals("Item3003", savedBehavior.getItemId().value());
        assertEquals(BehaviorType.CLICK, savedBehavior.getBehaviorType());
        assertEquals("192.168.1.1", savedBehavior.getDeviceInfo().clientIp());
        assertEquals("Mozilla/5.0", savedBehavior.getDeviceInfo().userAgent());
        assertEquals("https://google.com", savedBehavior.getReferrerUrl());
        assertEquals("1", savedBehavior.getMetadata().getValue("position"));
    }

    @Test
    void shouldThrowExceptionWhenSearchWithoutKeyword() {
        // Arrange (準備缺乏 keyword 的 SEARCH Command)
        LogBehaviorCommand command = new LogBehaviorCommand(
                "U1001",
                "S2002",
                null,
                "SEARCH", 
                "192.168.1.1",
                "Mozilla/5.0",
                "https://google.com",
                Map.of("wrong_key", "value"), // 遺失了 keyword
                Instant.now()
        );

        // Act & Assert (驗證領域例外拋出)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            applicationService.execute(command);
        });
        
        assertEquals("SEARCH behavior must have a keyword in metadata", exception.getMessage());
    }
}
