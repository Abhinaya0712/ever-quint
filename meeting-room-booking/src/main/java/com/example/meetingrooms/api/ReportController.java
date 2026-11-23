package com.example.meetingrooms.api;
import com.example.meetingrooms.api.ApiDtos.UtilizationItem;
import com.example.meetingrooms.service.UtilizationService;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.List;
@RestController @RequestMapping("/reports")
public class ReportController {
 private final UtilizationService service; public ReportController(UtilizationService service){this.service=service;}
 @GetMapping("/room-utilization") public List<UtilizationItem> utilization(@RequestParam OffsetDateTime from,@RequestParam OffsetDateTime to){return service.report(from,to);}
}
