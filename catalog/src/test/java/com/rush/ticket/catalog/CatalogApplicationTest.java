package com.rush.ticket.catalog;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CatalogApplicationTest {

    @Test
    void applicationStartsSuccessfully() {
        String[] args = new String[]{};

        try (MockedStatic<SpringApplication> springApplicationMock = Mockito.mockStatic(SpringApplication.class)) {
            springApplicationMock.when(() -> SpringApplication.run(CatalogApplication.class, args))
                    .thenReturn(Mockito.mock(ConfigurableApplicationContext.class));
            CatalogApplication.main(args);
            springApplicationMock.verify(() -> SpringApplication.run(CatalogApplication.class, args), Mockito.times(1));
        }
    }

    @Test
    void testConstructor() {
        CatalogApplication app = new CatalogApplication();
        assertNotNull(app);
    }
}
