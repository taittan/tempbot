package com.example.pdfreader.repository;

import com.example.pdfreader.entity.OdpBodyField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OdpBodyFieldRepository extends JpaRepository<OdpBodyField, Integer> {

    @Modifying
    @Query("DELETE FROM OdpBodyField")
    void deleteAllRecords();
}