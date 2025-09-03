```java
package com.example.subscription;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.example.subscription.repository")
public class SubscriptionRenewalApplication {
    public static void main(String[] args) {
        SpringApplication.run(SubscriptionRenewalApplication.class, args);

        // Khởi động Quartz Scheduler với cấu hình từ quartz.properties
        try {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
        } catch (SchedulerException e) {
            System.err.println("Failed to start Quartz Scheduler: " + e.getMessage());
        }
    }
}
```

##### Entity: Customer (TMF629)
<xaiArtifact artifact_id="50ee064e-8a11-4e7b-a38d-5918f3806560" artifact_version_id="cd12aff9-1924-445e-9032-963f90eea078" title="Customer.java" contentType="text/x-java-source">
```java
package com.example.subscription.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Customer {
    @Id
    private String id;
    private String name;
    private String customerCode; // Plain text, không mã hóa

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }
}
```

##### Entity: ProductOrder (TMF622)
<xaiArtifact artifact_id="94e93d51-29b8-4d14-b43a-08c006943004" artifact_version_id="e7482cad-0bc3-4fd7-84a4-a6570b32dbc8" title="ProductOrder.java" contentType="text/x-java-source">
```java
package com.example.subscription.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class ProductOrder {
    @Id
    private String id;
    private String state; // e.g., "acknowledged", "inProgress", "completed"
    private LocalDateTime orderDate;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "product_order_id")
    private List<ProductOrderItem> productOrderItem = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "product_order_id")
    private List<RelatedParty> relatedParty = new ArrayList<>();

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }
    public List<ProductOrderItem> getProductOrderItem() { return productOrderItem; }
    public void setProductOrderItem(List<ProductOrderItem> productOrderItem) { this.productOrderItem = productOrderItem; }
    public List<RelatedParty> getRelatedParty() { return relatedParty; }
    public void setRelatedParty(List<RelatedParty> relatedParty) { this.relatedParty = relatedParty; }
}
```

##### Entity: ProductOrderItem
<xaiArtifact artifact_id="f9834849-5de2-4d1e-8046-19021ba2a137" artifact_version_id="2063abab-daa4-48a9-bceb-511b76be36e7" title="ProductOrderItem.java" contentType="text/x-java-source">
```java
package com.example.subscription.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ProductOrderItem {
    @Id
    private String id;
    private String action; // e.g., "add", "modify", "delete"

    @Embedded
    private Product product;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}
```

##### Entity: Product
<xaiArtifact artifact_id="d8307bab-bf7e-4d2f-b02b-251ce7a58977" artifact_version_id="6c4c0a5a-2a28-435f-bf19-534c7d144cd2" title="Product.java" contentType="text/x-java-source">
```java
package com.example.subscription.entity;

import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;

@Embeddable
public class Product {
    private String productOfferingId; // ID gói cước
    private String productOfferingName; // Tên gói cước
    private String productCharacteristic; // Chi tiết gói cước, plain text
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // Getters and Setters
    public String getProductOfferingId() { return productOfferingId; }
    public void setProductOfferingId(String productOfferingId) { this.productOfferingId = productOfferingId; }
    public String getProductOfferingName() { return productOfferingName; }
    public void setProductOfferingName(String productOfferingName) { this.productOfferingName = productOfferingName; }
    public String getProductCharacteristic() { return productCharacteristic; }
    public void setProductCharacteristic(String productCharacteristic) { this.productCharacteristic = productCharacteristic; }
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
}
```

##### Entity: RelatedParty
<xaiArtifact artifact_id="e3e34c3b-48ee-4647-b7d8-a479431a8fd0" artifact_version_id="79076851-3d73-43b6-b13a-987d4f6bf785" title="RelatedParty.java" contentType="text/x-java-source">
```java
package com.example.subscription.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class RelatedParty {
    @Id
    private String id;
    private String role; // e.g., "Customer"
    private String name;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
```

##### Repository: Customer và ProductOrder
<xaiArtifact artifact_id="4e9208f9-4342-4ef5-96cd-d9c4f30be5fd" artifact_version_id="7025ba73-6ea5-4676-ae73-e51f8e46da7b" title="CustomerRepository.java" contentType="text/x-java-source">
```java
package com.example.subscription.repository;

import com.example.subscription.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, String> {
}
```

<xaiArtifact artifact_id="f5e4229a-1448-49b6-b23b-a8583759774e" artifact_version_id="86bc416f-edc6-44d2-9601-8bd600c99e97" title="ProductOrderRepository.java" contentType="text/x-java-source">
```java
package com.example.subscription.repository;

import com.example.subscription.entity.ProductOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductOrderRepository extends JpaRepository<ProductOrder, String> {
    @Query("SELECT po FROM ProductOrder po JOIN po.productOrderItem poi WHERE poi.product.endDate <= :thresholdDate AND po.state = 'completed'")
    List<ProductOrder> findExpiringProductOrders(LocalDateTime thresholdDate);
}
```

##### DTO: Validation đầu vào
<xaiArtifact artifact_id="c82bfec3-86bc-4231-9e4e-bb88cbc15384" artifact_version_id="88ce0bba-cf54-4e53-a738-a142aef6de67" title="ProductOrderRequest.java" contentType="text/x-java-source">
```java
package com.example.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class ProductOrderRequest {
    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotBlank(message = "Product offering ID is required")
    private String productOfferingId;

    @NotBlank(message = "Product offering name is required")
    private String productOfferingName;

    @NotBlank(message = "Product characteristic is required")
    private String productCharacteristic;

    @Positive(message = "Duration must be positive")
    private int durationDays;

    // Getters and Setters
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getProductOfferingId() { return productOfferingId; }
    public void setProductOfferingId(String productOfferingId) { this.productOfferingId = productOfferingId; }
    public String getProductOfferingName() { return productOfferingName; }
    public void setProductOfferingName(String productOfferingName) { this.productOfferingName = productOfferingName; }
    public String getProductCharacteristic() { return productCharacteristic; }
    public void setProductCharacteristic(String productCharacteristic) { this.productCharacteristic = productCharacteristic; }
    public int getDurationDays() { return durationDays; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }
}
```

<xaiArtifact artifact_id="cd18ed78-06b1-4a84-954c-06d40ff6e096" artifact_version_id="f5e7d6f7-a13c-482f-af23-c337afc54c37" title="RenewRequest.java" contentType="text/x-java-source">
```java
package com.example.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class RenewRequest {
    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotBlank(message = "Product offering ID is required")
    private String productOfferingId;

    @Positive(message = "Additional days must be positive")
    private int additionalDays;

    // Getters and Setters
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getProductOfferingId() { return productOfferingId; }
    public void setProductOfferingId(String productOfferingId) { this.productOfferingId = productOfferingId; }
    public int getAdditionalDays() { return additionalDays; }
    public void setAdditionalDays(int additionalDays) { this.additionalDays = additionalDays; }
}
```

##### Service: QuoteService (mô phỏng TMF648)
<xaiArtifact artifact_id="7afa0bfa-402d-4ece-8a08-023f9dee52d9" artifact_version_id="840bd31e-8837-4c36-99da-3ea6cb7ad0a2" title="QuoteService.java" contentType="text/x-java-source">
```java
package com.example.subscription.service;

public class QuoteService {
    // Mô phỏng TMF648 GET /quote/{id}: Kiểm tra báo giá
    public boolean validateQuote(String productOfferingId, int days) {
        // Giả lập: Báo giá hợp lệ nếu days > 0 và productOfferingId không rỗng
        return days > 0 && productOfferingId != null && !productOfferingId.isEmpty();
    }
}
```

##### Service: SubscriptionService
<xaiArtifact artifact_id="4461644c-d384-4cac-9519-de7816726eee" artifact_version_id="c0342fe3-3d59-4984-8f58-6728c62559d8" title="SubscriptionService.java" contentType="text/x-java-source">
```java
package com.example.subscription.service;

import com.example.subscription.entity.*;
import com.example.subscription.repository.CustomerRepository;
import com.example.subscription.repository.ProductOrderRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SubscriptionService {
    private final CustomerRepository customerRepository;
    private final ProductOrderRepository productOrderRepository;
    private final QuoteService quoteService;

    // Constructor injection
    public SubscriptionService(CustomerRepository customerRepository, ProductOrderRepository productOrderRepository, QuoteService quoteService) {
        this.customerRepository = customerRepository;
        this.productOrderRepository = productOrderRepository;
        this.quoteService = quoteService;
    }

    // Tạo khách hàng mới (TMF629)
    public Customer createCustomer(String name, String customerCode) {
        if (name == null || name.isEmpty() || customerCode == null || customerCode.isEmpty()) {
            throw new IllegalArgumentException("Name and customer code are required");
        }
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID().toString());
        customer.setName(name);
        customer.setCustomerCode(customerCode);
        customerRepository.save(customer);
        return customer;
    }

    // Tạo đơn hàng sản phẩm (TMF622 POST /productOrder)
    public ProductOrder createProductOrder(String customerId, String productOfferingId, String productOfferingName, String productCharacteristic, int durationDays) {
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        if (!customerOpt.isPresent()) {
            throw new IllegalArgumentException("Customer not found: " + customerId);
        }
        if (!quoteService.validateQuote(productOfferingId, durationDays)) {
            throw new IllegalArgumentException("Invalid quote for product offering: " + productOfferingId);
        }

        ProductOrder productOrder = new ProductOrder();
        productOrder.setId(UUID.randomUUID().toString());
        productOrder.setState("acknowledged");
        productOrder.setOrderDate(LocalDateTime.now());

        // Tạo ProductOrderItem
        ProductOrderItem item = new ProductOrderItem();
        item.setId(UUID.randomUUID().toString());
        item.setAction("add");

        Product product = new Product();
        product.setProductOfferingId(productOfferingId);
        product.setProductOfferingName(productOfferingName);
        product.setProductCharacteristic(productCharacteristic);
        product.setStartDate(LocalDateTime.now());
        product.setEndDate(LocalDateTime.now().plusDays(durationDays));

        item.setProduct(product);
        productOrder.getProductOrderItem().add(item);

        // Tạo RelatedParty
        RelatedParty relatedParty = new RelatedParty();
        relatedParty.setId(UUID.randomUUID().toString());
        relatedParty.setRole("Customer");
        relatedParty.setName(customerOpt.get().getName());
        productOrder.getRelatedParty().add(relatedParty);

        // Lưu trạng thái completed
        productOrder.setState("completed");
        productOrderRepository.save(productOrder);
        return productOrder;
    }

    // Gia hạn đơn hàng (TMF622 PATCH /productOrder/{id})
    public ProductOrder renewProductOrder(String customerId, String productOfferingId, int additionalDays) {
        List<ProductOrder> productOrders = productOrderRepository.findAll();
        ProductOrder productOrder = productOrders.stream()
                .filter(po -> po.getRelatedParty().stream().anyMatch(rp -> rp.getId().equals(customerId))
                        && po.getProductOrderItem().stream().anyMatch(poi -> poi.getProduct().getProductOfferingId().equals(productOfferingId)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Product order not found for customer: " + customerId + ", product offering: " + productOfferingId));

        if (!"completed".equals(productOrder.getState())) {
            throw new IllegalArgumentException("Product order cannot be renewed, current state: " + productOrder.getState());
        }
        if (!quoteService.validateQuote(productOfferingId, additionalDays)) {
            throw new IllegalArgumentException("Invalid quote for renewal: " + productOfferingId);
        }

        ProductOrderItem item = productOrder.getProductOrderItem().get(0); // Giả sử chỉ có 1 item
        item.setAction("modify");
        item.getProduct().setEndDate(item.getProduct().getEndDate().plusDays(additionalDays));

        productOrder.setState("completed");
        productOrderRepository.save(productOrder);
        return productOrder;
    }

    // Lấy thông tin đơn hàng (TMF622 GET /productOrder/{id})
    public ProductOrder getProductOrder(String productOrderId) {
        return productOrderRepository.findById(productOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Product order not found: " + productOrderId));
    }

    // Lấy danh sách đơn hàng của khách hàng
    public List<ProductOrder> getProductOrdersByCustomer(String customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new IllegalArgumentException("Customer not found: " + customerId);
        }
        return productOrderRepository.findAll().stream()
                .filter(po -> po.getRelatedParty().stream().anyMatch(rp -> rp.getId().equals(customerId)))
                .toList();
    }

    // Tìm đơn hàng sắp hết hạn (dùng cho scheduler)
    public List<ProductOrder> findExpiringProductOrders(LocalDateTime thresholdDate) {
        return productOrderRepository.findExpiringProductOrders(thresholdDate);
    }
}
```

##### Quartz Job: AutoRenewalJob
`AutoRenewalJob` tự khởi tạo `SubscriptionService` và các dependencies trong phương thức `execute`.

<xaiArtifact artifact_id="8aa3231f-9cc3-4643-8eeb-c07769fde4bc" artifact_version_id="eb7f6b8c-29e6-4791-b311-4de49b59b669" title="AutoRenewalJob.java" contentType="text/x-java-source">
```java
package com.example.subscription.scheduler;

import com.example.subscription.entity.ProductOrder;
import com.example.subscription.repository.CustomerRepository;
import com.example.subscription.repository.ProductOrderRepository;
import com.example.subscription.service.QuoteService;
import com.example.subscription.service.SubscriptionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import java.time.LocalDateTime;
import java.util.List;

public class AutoRenewalJob implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // Khởi tạo EntityManager và repositories thủ công
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("subscriptionPU");
        EntityManager em = emf.createEntityManager();
        RepositoryFactorySupport factory = new JpaRepositoryFactory(em);
        CustomerRepository customerRepository = factory.getRepository(CustomerRepository.class);
        ProductOrderRepository productOrderRepository = factory.getRepository(ProductOrderRepository.class);

        // Khởi tạo services thủ công
        QuoteService quoteService = new QuoteService();
        SubscriptionService subscriptionService = new SubscriptionService(customerRepository, productOrderRepository, quoteService);

        // Tìm các đơn hàng sắp hết hạn trong 2 ngày
        LocalDateTime thresholdDate = LocalDateTime.now().plusDays(2);
        List<ProductOrder> expiringProductOrders = subscriptionService.findExpiringProductOrders(thresholdDate);

        for (ProductOrder productOrder : expiringProductOrders) {
            try {
                String customerId = productOrder.getRelatedParty().get(0).getId();
                String productOfferingId = productOrder.getProductOrderItem().get(0).getProduct().getProductOfferingId();
                // Gia hạn tự động thêm 30 ngày
                subscriptionService.renewProductOrder(customerId, productOfferingId, 30);
                System.out.println("Auto-renewed product order: " + productOrder.getId() + " for customer: " + customerId);
            } catch (IllegalArgumentException e) {
                System.err.println("Failed to auto-renew product order: " + productOrder.getId() + ", error: " + e.getMessage());
            }
        }

        // Đóng EntityManager
        em.close();
        emf.close();
    }
}
```

##### Controller: SubscriptionController
<xaiArtifact artifact_id="160e574e-3306-4d29-ad58-f07397915089" artifact_version_id="2fa98ce4-6b95-4e2a-a2bc-ed6e9f793d95" title="SubscriptionController.java" contentType="text/x-java-source">
```java
package com.example.subscription.controller;

import com.example.subscription.dto.ProductOrderRequest;
import com.example.subscription.dto.RenewRequest;
import com.example.subscription.entity.Customer;
import com.example.subscription.entity.ProductOrder;
import com.example.subscription.repository.CustomerRepository;
import com.example.subscription.repository.ProductOrderRepository;
import com.example.subscription.service.QuoteService;
import com.example.subscription.service.SubscriptionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.validation.Valid;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

@RestController
@RequestMapping("/api/productOrder")
@Validated
public class SubscriptionController {
    private final SubscriptionService service;

    // Constructor injection với khởi tạo thủ công
    public SubscriptionController() {
        // Khởi tạo EntityManager và repositories
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("subscriptionPU");
        EntityManager em = emf.createEntityManager();
        RepositoryFactorySupport factory = new JpaRepositoryFactory(em);
        CustomerRepository customerRepository = factory.getRepository(CustomerRepository.class);
        ProductOrderRepository productOrderRepository = factory.getRepository(ProductOrderRepository.class);

        // Khởi tạo services
        QuoteService quoteService = new QuoteService();
        this.service = new SubscriptionService(customerRepository, productOrderRepository, quoteService);
    }

    // TMF629 POST /customer
    @PostMapping("/customer")
    public ResponseEntity<Customer> createCustomer(@RequestParam String name, @RequestParam String customerCode) {
        Customer customer = service.createCustomer(name, customerCode);
        return ResponseEntity.ok(customer);
    }

    // TMF622 POST /productOrder
    @PostMapping
    public ResponseEntity<ProductOrder> createProductOrder(@Valid @RequestBody ProductOrderRequest request) {
        ProductOrder productOrder = service.createProductOrder(
                request.getCustomerId(),
                request.getProductOfferingId(),
                request.getProductOfferingName(),
                request.getProductCharacteristic(),
                request.getDurationDays()
        );
        return ResponseEntity.ok(productOrder);
    }

    // TMF622 PATCH /productOrder/renew
    @PatchMapping("/renew")
    public ResponseEntity<ProductOrder> renewProductOrder(@Valid @RequestBody RenewRequest request) {
        ProductOrder productOrder = service.renewProductOrder(
                request.getCustomerId(),
                request.getProductOfferingId(),
                request.getAdditionalDays()
        );
        return ResponseEntity.ok(productOrder);
    }

    // TMF622 GET /productOrder/{id}
    @GetMapping("/{productOrderId}")
    public ResponseEntity<ProductOrder> getProductOrder(@PathVariable String productOrderId) {
        ProductOrder productOrder = service.getProductOrder(productOrderId);
        return ResponseEntity.ok(productOrder);
    }

    // GET danh sách đơn hàng của khách hàng
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<ProductOrder>> getProductOrdersByCustomer(@PathVariable String customerId) {
        List<ProductOrder> productOrders = service.getProductOrdersByCustomer(customerId);
        return ResponseEntity.ok(productOrders);
    }

    // Xử lý lỗi validation
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        return new ResponseEntity<>(ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage(), HttpStatus.BAD_REQUEST);
    }

    // Xử lý lỗi chung
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentExceptions(IllegalArgumentException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }
}
```

##### Cấu hình Persistence (cho H2 Database)
<xaiArtifact artifact_id="da5cd7a2-403c-4fbc-9f88-8b8d3c347ae6" artifact_version_id="272ca160-ea52-4547-b380-101d839b0a00" title="persistence.xml" contentType="text/xml">
```xml
<persistence version="2.1" xmlns="http://xmlns.jcp.org/xml/ns/persistence"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/persistence
                                 http://xmlns.jcp.org/xml/ns/persistence/persistence_2_1.xsd">
    <persistence-unit name="subscriptionPU" transaction-type="RESOURCE_LOCAL">
        <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
        <class>com.example.subscription.entity.Customer</class>
        <class>com.example.subscription.entity.ProductOrder</class>
        <class>com.example.subscription.entity.ProductOrderItem</class>
        <class>com.example.subscription.entity.RelatedParty</class>
        <properties>
            <property name="javax.persistence.jdbc.url" value="jdbc:h2:mem:subscription"/>
            <property name="javax.persistence.jdbc.driver" value="org.h2.Driver"/>
            <property name="javax.persistence.jdbc.user" value="sa"/>
            <property name="javax.persistence.jdbc.password" value=""/>
            <property name="hibernate.dialect" value="org.hibernate.dialect.H2Dialect"/>
            <property name="hibernate.hbm2ddl.auto" value="update"/>
        </properties>
    </persistence-unit>
</persistence>
```
