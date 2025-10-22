package org.ide.hack1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name = "sales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sale {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private int units;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private String branch;

    @Column(nullable = false)
    private Instant soldAt;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null || this.id.isBlank()) {
            this.id = "s_" + UUID.randomUUID().toString().replace("-", "");
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

}
