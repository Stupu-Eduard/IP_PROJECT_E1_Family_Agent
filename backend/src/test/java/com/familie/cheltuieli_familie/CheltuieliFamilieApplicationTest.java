package com.familie.cheltuieli_familie;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CheltuieliFamilieApplicationTest {

    @Test
    void classHasSpringBootApplicationAnnotation() {
        assertTrue(
                CheltuieliFamilieApplication.class.isAnnotationPresent(SpringBootApplication.class)
        );
    }

    @Test
    void mainCallsSpringApplicationRun() {
        String[] args = {"--server.port=0"};

        try (MockedStatic<SpringApplication> springAppMock = mockStatic(SpringApplication.class)) {
            ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);

            springAppMock
                    .when(() -> SpringApplication.run(eq(CheltuieliFamilieApplication.class), eq(args)))
                    .thenReturn(context);

            CheltuieliFamilieApplication.main(args);

            springAppMock.verify(
                    () -> SpringApplication.run(CheltuieliFamilieApplication.class, args),
                    times(1)
            );
        }
    }
}