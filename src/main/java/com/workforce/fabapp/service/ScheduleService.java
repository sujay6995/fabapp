package com.workforce.fabapp.service;

import com.workforce.fabapp.dto.ScheduleDayDto;
import com.workforce.fabapp.entity.CrewSchedule;
import com.workforce.fabapp.entity.Employee;
import com.workforce.fabapp.entity.LeaveRequest;
import com.workforce.fabapp.enums.LeaveStatus;
import com.workforce.fabapp.repository.CrewScheduleRepository;
import com.workforce.fabapp.repository.EmployeeRepository;
import com.workforce.fabapp.repository.LeaveRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final EmployeeRepository employeeRepository;
    private final CrewScheduleRepository crewScheduleRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public List<ScheduleDayDto> getMonthlySchedule(Long employeeId, String month) {
        Employee employee = employeeRepository.findByIdWithProfile(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        YearMonth ym = YearMonth.parse(month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        Long crewId = employee.getCrew().getId();
        String crewName = employee.getCrew().getName();

        Map<LocalDate, CrewSchedule> schedulesByDate = crewScheduleRepository
                .findByCrewIdAndWorkDateBetween(crewId, start, end)
                .stream()
                .collect(Collectors.toMap(CrewSchedule::getWorkDate, schedule -> schedule));

        List<LeaveRequest> approvedLeaves = leaveRequestRepository
                .findByEmployeeIdAndStatusOverlapping(employeeId, LeaveStatus.APPROVED, start, end);

        List<ScheduleDayDto> result = new ArrayList<>();

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {

            LocalDate currentDate = d;

            CrewSchedule shift = schedulesByDate.get(currentDate);
            boolean workday = shift != null && Boolean.TRUE.equals(shift.getIsWorkday());

            LeaveRequest leave = approvedLeaves.stream()
                    .filter(l -> l.getStatus() == LeaveStatus.APPROVED
                            && !currentDate.isBefore(l.getStartDate())
                            && !currentDate.isAfter(l.getEndDate()))
                    .findFirst()
                    .orElse(null);

            result.add(ScheduleDayDto.builder()
                    .date(currentDate)
                    .workday(workday)
                    .code(workday ? "D" : "OFF")
                    .label(workday ? crewName + " workday"
                            : crewName + " day off")
                    .hasApprovedLeave(leave != null)
                    .leaveTypeName(leave != null ? leave.getLeaveType().getName() : null)
                    .build());
        }

        return result;
    }
}
