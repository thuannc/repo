import com.example.subscription.repository.CustomerRepository;
import com.example.subscription.repository.ProductOrderRepository;
import com.example.subscription.scheduler.AutoRenewalJob;
import com.example.subscription.service.QuoteService;
import com.example.subscription.service.SubscriptionService;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.example.subscription.repository")
public class SubscriptionRenewalApplication {
    public static void main(String[] args) {
        SpringApplication.run(SubscriptionRenewalApplication.class, args);

        // Khởi tạo EntityManager và repositories thủ công
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("subscriptionPU");
        EntityManager em = emf.createEntityManager();
        RepositoryFactorySupport factory = new JpaRepositoryFactory(em);
        CustomerRepository customerRepository = factory.getRepository(CustomerRepository.class);
        ProductOrderRepository productOrderRepository = factory.getRepository(ProductOrderRepository.class);

        // Khởi tạo services thủ công
        QuoteService quoteService = new QuoteService();
        SubscriptionService subscriptionService = new SubscriptionService(customerRepository, productOrderRepository, quoteService);

        // Khởi động Quartz Scheduler với cấu hình từ quartz.properties
        try {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

            // Truyền SubscriptionService qua JobDataMap
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("subscriptionService", subscriptionService);

            // Cập nhật JobDataMap cho job đã định nghĩa trong quartz.properties
            JobDetail job = JobBuilder.newJob(AutoRenewalJob.class)
                    .withIdentity("autoRenewalJob", "group1")
                    .usingJobData(jobDataMap)
                    .build();

            // Lập lịch job (trigger đã được cấu hình trong quartz.properties)
            scheduler.scheduleJob(job);
            scheduler.start();
        } catch (SchedulerException e) {
            System.err.println("Failed to start Quartz Scheduler: " + e.getMessage());
        }
    }
}
```

##### Entity: Customer (TMF629)
<xaiArtifact artifact_id="58585013-effc-44e7-ad9a-cac1c6ccc664" artifact_version_id="f75ddb8f-795b-4238-badb-8f0c2e092dc0" title="Customer.java" contentType="text/x-java-source">
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
<xaiArtifact artifact_id="e91a679c-242a-436b-a0aa-bb17bf410fa8" artifact_version_id="bd3da245-fd07-490f-98e2-ec6599d5bfbd" title="ProductOrder.java" contentType="text/x-java-source">
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
<xaiArtifact artifact_id="5428ab26-3091-4f97-a586-29c8d7117c79" artifact_version_id="f05fe739-40b5-4934-b28a-c78452609198" title="ProductOrderItem.java" contentType="text/x-java-source">
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
<xaiArtifact artifact_id="ff85ec40-d03e-48af-9a0d-8fee6277440a" artifact_version_id="e9548394-05a2-419d-b904-d19e4a30f769" title="Product.java" contentType="text/x-java-source">
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
<xaiArtifact artifact_id="563f9bc5-c82e-40f4-86dc-c6a37ce3abaf" artifact_version_id="32a51258-c2ea-487a-85cc-fc51d4fa05cb" title="RelatedParty.java" contentType="text/x-java-source">
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
<xaiArtifact artifact_id="2548ff12-be80-4e9f-9fef-aa0f4514045e" artifact_version_id="31faf54e-e887-4e86-ac3e-1e0355fea17a" title="CustomerRepository.java" contentType="text/x-java-source">
```java
package com.example.subscription.repository;

import com.example.subscription.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, String> {
}
```

<xaiArtifact artifact_id="e4bfba99-4b61-4098-aeb1-8f9b73afbd8d" artifact_version_id="a50d473d-8115-44a4-a845-f418e477beca" title="ProductOrderRepository.java" contentType="text/x-java-source">
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
<xaiArtifact artifact_id="f12b94e4-6e46-4790-bd07-659171dcfae4" artifact_version_id="090b3e84-03c1-4beb-a40f-f283375659aa" title="ProductOrderRequest.java" contentType="text/x-java-source">
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

<xaiArtifact artifact_id="71b58215-1506-47fe-8ea6-8913ee4b52fe" artifact_version_id="40f9f017-978a-496a-91fb-d1642ce46758" title="RenewRequest.java" contentType="text/x-java-source">
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
<xaiArtifact artifact_id="a80d88c5-56fb-4706-8fbc-701a7d480aa0" artifact_version_id="6955eccd-4cc8-45e5-a271-ec1f81ae09a5" title="QuoteService.java" contentType="text/x-java-source">
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
<xaiArtifact artifact_id="558eed92-507c-41f8-b88b-1d82b46a6c03" artifact_version_id="939375df-534d-4182-ba39-818a35417ccc" title="SubscriptionService.java" contentType="text/x-java-source">
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
<xaiArtifact artifact_id="8c78beda-d78d-4dab-820b-4b789c2dda69" artifact_version_id="53bd7f00-6c91-4d44-a466-90000d80af2b" title="AutoRenewalJob.java" contentType="text/x-java-source">
```java
package com.example.subscription.scheduler;

import com.example.subscription.entity.ProductOrder;
import com.example.subscription.service.SubscriptionService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.time.LocalDateTime;
import java.util.List;

public class AutoRenewalJob implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // Lấy SubscriptionService từ JobDataMap
        SubscriptionService subscriptionService = (SubscriptionService) context.getMergedJobDataMap().get("subscriptionService");

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
    }
}
```

##### Controller: SubscriptionController
<xaiArtifact artifact_id="7abcb1f7-f33f-4aa7-b0d2-91b7da3983a1" artifact_version_id="038845f7-4e36-49ee-a789-b8194ef91024" title="SubscriptionController.java" contentType="text/x-java-source">
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
<xaiArtifact artifact_id="3f1f3508-e3de-41cc-aa67-d5d42872b972" artifact_version_id="3acd11a0-c9a9-4840-90f4-00579b4d0709" title="persistence.xml" contentType="text/xml">
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
