package com.example.scaffold.audit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig
class AuditableAspectTest {

    @Mock
    private AuditService auditService;

    private AuditableAspect auditableAspect;
    private TestService testService;
    private TestService proxiedTestService;

    @BeforeEach
    void setUp() {
        auditableAspect = new AuditableAspect(auditService);
        testService = new TestService();
        
        // Create proxy with aspect
        AspectJProxyFactory factory = new AspectJProxyFactory(testService);
        factory.addAspect(auditableAspect);
        proxiedTestService = factory.getProxy();
    }

    @Test
    void auditableAnnotation_OnMethodExecution_ShouldLogEvent() {
        // Act
        TestEntity result = proxiedTestService.createEntity("Test Name");

        // Assert
        verify(auditService).logEvent(
                eq("TEST_ENTITY"),
                eq(123L),
                eq("CREATE"),
                any()
        );
    }

    @Test
    void auditableAnnotation_WithLogBefore_ShouldLogBeforeExecution() {
        // Act
        proxiedTestService.deleteEntity(456L);

        // Assert
        verify(auditService).logEvent(
                eq("TEST_ENTITY"),
                eq(456L),
                eq("DELETE"),
                any()
        );
    }

    @Test
    void auditableAnnotation_WithDetailsExpression_ShouldExtractCustomDetails() {
        // Act
        TestEntity result = proxiedTestService.updateEntity(789L, "Updated Name");

        // Assert
        verify(auditService).logEvent(
                eq("TEST_ENTITY"),
                eq(789L),
                eq("UPDATE"),
                argThat(details -> details != null && details.toString().contains("Updated Name"))
        );
    }

    // Test service class with auditable methods
    static class TestService {
        
        @Auditable(entityType = "TEST_ENTITY", eventType = "CREATE", 
                  entityIdExpression = "#result.id")
        public TestEntity createEntity(String name) {
            TestEntity entity = new TestEntity();
            entity.setId(123L);
            entity.setName(name);
            return entity;
        }
        
        @Auditable(entityType = "TEST_ENTITY", eventType = "DELETE", 
                  entityIdExpression = "#id", logBefore = true, logAfter = false)
        public void deleteEntity(Long id) {
            // Simulate deletion
        }
        
        @Auditable(entityType = "TEST_ENTITY", eventType = "UPDATE",
                  entityIdExpression = "#id", detailsExpression = "#name")
        public TestEntity updateEntity(Long id, String name) {
            TestEntity entity = new TestEntity();
            entity.setId(id);
            entity.setName(name);
            return entity;
        }
    }

    // Test entity class
    static class TestEntity {
        private Long id;
        private String name;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}