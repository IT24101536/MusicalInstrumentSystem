package org.example.musicalinstrumentsystem.service;

import org.example.musicalinstrumentsystem.entity.FinancialRecord;
import org.example.musicalinstrumentsystem.repository.FinancialRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FinancialRecordService {

    @Autowired
    private FinancialRecordRepository financialRecordRepository;

    public List<FinancialRecord> getAllRecords() {
        return financialRecordRepository.findAll();
    }

    public FinancialRecord saveRecord(FinancialRecord record) {
        return financialRecordRepository.save(record);
    }

    public Optional<FinancialRecord> getRecordById(Long id) {
        return financialRecordRepository.findById(id);
    }

    public boolean deleteRecord(Long id) {
        if (financialRecordRepository.existsById(id)) {
            financialRecordRepository.deleteById(id);
            return true;
        }
        return false;
}
}
