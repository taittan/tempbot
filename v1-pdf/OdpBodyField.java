package com.example.pdfreader.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ODP_BODY_FIELDS")
public class OdpBodyField {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "odp_body_fields_seq")
    @SequenceGenerator(name = "odp_body_fields_seq", sequenceName = "ODP_BODY_FIELDS_SEQ", allocationSize = 1)
    @Column(name = "FIELD_NUMBER", precision = 3)
    private Integer fieldNumber;

    @Column(name = "FIELD_NAME", length = 80)
    private String fieldName;

    @Column(name = "DATA_TYPE", length = 20)
    private String dataType;

    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    @Override
    public String toString() {
        return String.format("| %-5s | %-30s | %-15s | %-50s |",
                fieldNumber != null ? fieldNumber : "",
                fieldName != null ? fieldName : "",
                dataType != null ? dataType : "",
                description != null ? (description.length() > 50 ? description.substring(0, 47) + "..." : description) : "");
    }
}