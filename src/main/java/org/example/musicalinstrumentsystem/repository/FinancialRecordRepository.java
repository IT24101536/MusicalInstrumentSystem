package org.example.musicalinstrumentsystem.repository;

import org.example.musicalinstrumentsystem.entity.FinancialRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinancialRecordRepository extends JpaRepository<FinancialRecord,Long>{
        }