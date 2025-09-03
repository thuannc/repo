package com.example.subscription;

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

        // Khởi động Quartz Scheduler
        try {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

            // Định nghĩa Job và truyền SubscriptionService qua JobDataMap
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("subscriptionService", subscriptionService);
            JobDetail job = JobBuilder.newJob(AutoRenewalJob.class)
                    .withIdentity("autoRenewalJob", "group1")
                    .usingJobData(jobDataMap)
                    .build();

            // Định nghĩa Trigger với cú pháp crontab (1:00 AM hàng ngày)
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("autoRenewalTrigger", "group1")
                    .withSchedule(CronScheduleBuilder.cronSchedule("0 0 1 * * ?"))
                    .build();

            // Lập lịch job
            scheduler.scheduleJob(job, trigger);
            scheduler.start();
        } catch (SchedulerException e) {
            System.err.println("Failed to start Quartz Scheduler: " + e.getMessage());
        }
    }
}
```

##### Entity: Customer (TMF629)
<xaiArtifact artifact_id="99b7331a-4f4e-4325-8d27-fa77ba661943" artifact_version_id="80f8cd49-6866-411e-a68a-fab318781d90" title="Customer.java" contentType="text/x-java-source">
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
Entity này được thiết kế để tuân thủ chuẩn TMF622, với các trường và quan hệ cần thiết.

<xaiArtifact artifact_id="f887634c-4866-42d7-984e-29ef61a47361" artifact_version_id="688bc029-5828-4ad8-aeda-9c0e7a97924f" title="ProductOrder.java" contentType="text/x-java-source">
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
<xaiArtifact artifact_id="cba038f6-973a-4ebe-9cc0-57545654067d" artifact_version_id="43eec107-1658-4efd-88df-0457536ac71f" title="ProductOrderItem.java" contentType="text/x-java-source">
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
<xaiArtifact artifact_id="3ec7cf02-07ee-4fa8-8aec-7577b7595e48" artifact_version_id="290c30e7-c035-4f36-8d7d-c10ffa06cd4c" title="Product.java" contentType="text/x-java-source">
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
<xaiArtifact artifact_id="23fb1746-1572-4169-9ea1-760937063ab7" artifact_version_id="f6190110-a3bc-4357-ac0b-15274fbda30a" title="RelatedParty.java" contentType="text/x-java-source">
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
<xaiArtifact artifact_id="57bf120d-b462-43d3-b9d3-f62e25c590ac" artifact_version_id="b704a09c-fda6-4037-ab2d-84449ffc71b0" title="CustomerRepository.java" contentType="text/x-java-source">
```java
package com.example.subscription.repository;

import com.example.subscription.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, String> {
}
```

<xaiArtifact artifact_id="de1530d2-18d3-4101-8421-46b120737463" artifact_version_id="f3473282-5009-42e0-a134-546ed3633b48" title="ProductOrderRepository.java" contentType="text/x-java-source">
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
Cập nhật DTO để phù hợp với cấu trúc TMF622.

<xaiArtifact artifact_id="4af7e87f-75c9-4d31-b175-6d3e50fd1a9f" artifact_version_id="be8c5d6a-da9d-4bf6-af48-6274659f5abd" title="ProductOrderRequest.java" contentType="text/x-java-source">
```java
package com.example.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

<xaiArtifact artifact_id="9e61015b-991c-4a34-966d-b6673d7171a4" artifact_version_id="469e7041-5670-4c27-816a-86d93645cb25" title="RenewRequest.java" contentType="text/x-java-source">
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
<xaiArtifact artifact_id="1170f450-6e2b-4465-b378-aa72b3d0f44c" artifact_version_id="46da60e4-85dd-4133-8126-038e99540a04" title="QuoteService.java" contentType="text/x-java-source">
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
Cập nhật để xử lý `ProductOrder` theo chuẩn TMF622.

<xaiArtifact artifact_id="3f2b52bb-4bd9-4e59-a6e0-72d18d2ee8b0" artifact_version_id="b4e78e6d-3517-4c43-af93-fadebc9d1173" title="SubscriptionService.java" contentType="text/x-java-source">
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
<xaiArtifact artifact_id="d9ab8b09-b97f-4ccf-bfff-cae4b1fe3b07" artifact_version_id="24b2fc5c-f85e-4932-bbf9-9410cf3b6cb0" title="AutoRenewalJob.java" contentType="text/x-java-source">
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
<xaiArtifact artifact_id="02769cb3-7d28-42ad-82b5-8cd5252ab0d0" artifact_version_id="747fe199-f65f-4ff0-b7ae-4ad6a1bad58b" title="SubscriptionController.java" contentType="text/x-java-source">
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

    // TMF622 PATCH /productOrder/{id}
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
<xaiArtifact artifact_id="a4a57cf6-03dc-4ce4-a10f-92d0ee76bc47" artifact_version_id="f924e033-e58e-4d4d-855b-f85eb27b7139" title="persistence.xml" contentType="text/xml">
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
