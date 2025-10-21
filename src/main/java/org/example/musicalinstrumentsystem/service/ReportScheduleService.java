package org.example.musicalinstrumentsystem.service;

import org.example.musicalinstrumentsystem.entity.ReportSchedule;
import org.example.musicalinstrumentsystem.entity.User;
import org.example.musicalinstrumentsystem.repository.ReportScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReportScheduleService {

    @Autowired
    private ReportScheduleRepository reportScheduleRepository;

    public ReportSchedule createSchedule(
            String scheduleName,
            String reportType,
            String frequency,
            Integer dayOfWeek,
            Integer dayOfMonth,
            String emailRecipients,
            String description,
            User createdBy) {

        try {
            System.out.println("=== CREATING REPORT SCHEDULE ===");
            System.out.println("Schedule Name: " + scheduleName);
            System.out.println("Frequency: " + frequency);

            ReportSchedule schedule = new ReportSchedule();
            schedule.setScheduleName(scheduleName);
            schedule.setReportType(reportType);
            schedule.setFrequency(frequency);
            schedule.setDayOfWeek(dayOfWeek);
            schedule.setDayOfMonth(dayOfMonth);
            schedule.setEmailRecipients(emailRecipients);
            schedule.setDescription(description);
            schedule.setCreatedBy(createdBy);
            schedule.setIsActive(true);
            schedule.setCreatedAt(LocalDateTime.now());
            schedule.setNextExecution(calculateNextExecution(frequency, dayOfWeek, dayOfMonth));

            ReportSchedule savedSchedule = reportScheduleRepository.save(schedule);
            System.out.println(" Schedule created with ID: " + savedSchedule.getId());
            return savedSchedule;

        } catch (Exception e) {
            System.out.println(" Error creating schedule: " + e.getMessage());
            throw new RuntimeException("Failed to create schedule", e);
        }
    }

    public List<ReportSchedule> getAllSchedules() {
        try {
            System.out.println("=== FETCHING ALL SCHEDULES ===");
            List<ReportSchedule> schedules = reportScheduleRepository.findAll();
            System.out.println(" Found " + schedules.size() + " schedules");
            return schedules;
        } catch (Exception e) {
            System.out.println(" Error fetching schedules: " + e.getMessage());
            return List.of();
        }
    }

    public List<ReportSchedule> getActiveSchedules() {
        try {
            System.out.println("=== FETCHING ACTIVE SCHEDULES ===");
            return reportScheduleRepository.findByIsActiveTrueOrderByNextExecutionAsc();
        } catch (Exception e) {
            System.out.println(" Error fetching active schedules: " + e.getMessage());
            return List.of();
        }
    }

    public Optional<ReportSchedule> getScheduleById(Long id) {
        try {
            System.out.println("=== FETCHING SCHEDULE BY ID: " + id + " ===");
            return reportScheduleRepository.findById(id);
        } catch (Exception e) {
            System.out.println(" Error fetching schedule: " + e.getMessage());
            return Optional.empty();
        }
    }

    public List<ReportSchedule> getSchedulesByUser(User user) {
        try {
            System.out.println("=== FETCHING SCHEDULES BY USER: " + user.getEmail() + " ===");
            return reportScheduleRepository.findByCreatedByOrderByCreatedAtDesc(user);
        } catch (Exception e) {
            System.out.println(" Error fetching user schedules: " + e.getMessage());
            return List.of();
        }
    }

    public List<ReportSchedule> getSchedulesByFrequency(String frequency) {
        try {
            System.out.println("=== FETCHING SCHEDULES BY FREQUENCY: " + frequency + " ===");
            return reportScheduleRepository.findByFrequencyOrderByCreatedAtDesc(frequency);
        } catch (Exception e) {
            System.out.println(" Error fetching schedules by frequency: " + e.getMessage());
            return List.of();
        }
    }

    public long countActiveSchedules() {
        return reportScheduleRepository.countByIsActiveTrue();
    }

    public ReportSchedule updateSchedule(
            Long id,
            String scheduleName,
            String reportType,
            String frequency,
            Integer dayOfWeek,
            Integer dayOfMonth,
            String emailRecipients,
            String description,
            Boolean isActive) {

        try {
            System.out.println("=== UPDATING SCHEDULE ID: " + id + " ===");
            Optional<ReportSchedule> scheduleOpt = reportScheduleRepository.findById(id);

            if (scheduleOpt.isPresent()) {
                ReportSchedule schedule = scheduleOpt.get();

                if (scheduleName != null && !scheduleName.isEmpty()) {
                    schedule.setScheduleName(scheduleName);
                }
                if (reportType != null) {
                    schedule.setReportType(reportType);
                }
                if (frequency != null) {
                    schedule.setFrequency(frequency);
                    schedule.setNextExecution(calculateNextExecution(frequency, dayOfWeek, dayOfMonth));
                }
                if (dayOfWeek != null) {
                    schedule.setDayOfWeek(dayOfWeek);
                }
                if (dayOfMonth != null) {
                    schedule.setDayOfMonth(dayOfMonth);
                }
                if (emailRecipients != null) {
                    schedule.setEmailRecipients(emailRecipients);
                }
                if (description != null) {
                    schedule.setDescription(description);
                }
                if (isActive != null) {
                    schedule.setIsActive(isActive);
                }

                ReportSchedule updated = reportScheduleRepository.save(schedule);
                System.out.println(" Schedule updated successfully");
                return updated;
            } else {
                System.out.println(" Schedule not found");
                return null;
            }
        } catch (Exception e) {
            System.out.println(" Error updating schedule: " + e.getMessage());
            throw new RuntimeException("Failed to update schedule", e);
        }
    }

    public ReportSchedule toggleScheduleStatus(Long id) {
        try {
            System.out.println("=== TOGGLING SCHEDULE STATUS: " + id + " ===");
            Optional<ReportSchedule> scheduleOpt = reportScheduleRepository.findById(id);

            if (scheduleOpt.isPresent()) {
                ReportSchedule schedule = scheduleOpt.get();
                schedule.setIsActive(!schedule.getIsActive());

                ReportSchedule updated = reportScheduleRepository.save(schedule);
                System.out.println(" Schedule status toggled: " + updated.getIsActive());
                return updated;
            } else {
                System.out.println(" Schedule not found");
                return null;
            }
        } catch (Exception e) {
            System.out.println(" Error toggling schedule: " + e.getMessage());
            return null;
        }
    }

    public boolean deleteSchedule(Long id) {
        try {
            System.out.println("=== DELETING SCHEDULE ID: " + id + " ===");
            if (reportScheduleRepository.existsById(id)) {
                reportScheduleRepository.deleteById(id);
                System.out.println(" Schedule deleted successfully");
                return true;
            } else {
                System.out.println(" Schedule not found");
                return false;
            }
        } catch (Exception e) {
            System.out.println(" Error deleting schedule: " + e.getMessage());
            return false;
        }
    }

    private LocalDateTime calculateNextExecution(String frequency, Integer dayOfWeek, Integer dayOfMonth) {
        LocalDateTime now = LocalDateTime.now();

        switch (frequency) {
            case "DAILY":
                return now.plusDays(1).withHour(8).withMinute(0).withSecond(0);

            case "WEEKLY":
                int daysToAdd = (dayOfWeek != null) ? dayOfWeek : 7;
                return now.plusDays(daysToAdd).withHour(8).withMinute(0).withSecond(0);

            case "MONTHLY":
                int targetDay = (dayOfMonth != null) ? dayOfMonth : 1;
                LocalDateTime nextMonth = now.plusMonths(1).withDayOfMonth(1);
                return nextMonth.withDayOfMonth(Math.min(targetDay, nextMonth.toLocalDate().lengthOfMonth()))
                        .withHour(8).withMinute(0).withSecond(0);

            default:
                return now.plusDays(1).withHour(8).withMinute(0).withSecond(0);
        }
    }
}

