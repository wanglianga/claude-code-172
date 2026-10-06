package com.example.robotdelivery.event;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "统一配送事件", description = "同一配送事件流：设备动作与业务结果的统一时间线")
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final DeliveryEventRepository eventRepository;

    public EventController(DeliveryEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Operation(summary = "按社区/类型查询统一配送事件流")
    @GetMapping
    public List<DeliveryEvent> query(@RequestParam(required = false) Long communityId,
                                     @RequestParam(required = false) String eventType,
                                     @RequestParam(defaultValue = "100") int limit) {
        PageRequest pageable = PageRequest.of(0, Math.min(Math.max(limit, 1), 500));
        if (communityId != null && eventType != null) {
            return eventRepository.findByCommunityIdAndEventTypeOrderByOccurredAtDesc(
                    communityId, eventType, pageable);
        }
        if (communityId != null) {
            return eventRepository.findByCommunityIdOrderByOccurredAtDesc(communityId, pageable);
        }
        return eventRepository.findAllByOrderByOccurredAtDesc(pageable);
    }
}
