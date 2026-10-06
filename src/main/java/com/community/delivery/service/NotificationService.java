package com.community.delivery.service;

import com.community.delivery.domain.entity.DeliveryOrder;
import com.community.delivery.domain.entity.NotificationRecord;
import com.community.delivery.domain.enums.NotificationChannel;
import com.community.delivery.domain.repository.NotificationRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 用户通知服务：到达通知、超时提醒、异常告知。
 * 有小程序标识优先小程序推送，否则短信。记录业务侧通知留痕。
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRecordRepository repository;

    public NotificationService(NotificationRecordRepository repository) {
        this.repository = repository;
    }

    public NotificationRecord notify(DeliveryOrder order, String notifyType, String content) {
        NotificationChannel channel = order.getMiniAppUserId() != null
                ? NotificationChannel.MINI_APP : NotificationChannel.SMS;

        NotificationRecord record = new NotificationRecord();
        record.setOrderNo(order.getOrderNo());
        record.setChannel(channel);
        record.setPhone(order.getUserPhone());
        record.setContent(content);
        record.setNotifyType(notifyType);
        // 真实环境在此调用短信/小程序网关；当前模拟始终成功
        record.setSuccess(true);
        NotificationRecord saved = repository.save(record);

        log.info("[{}通知] orderNo={} -> {} ({}) {}",
                channel.getLabel(), order.getOrderNo(), order.getUserPhone(), notifyType, content);
        return saved;
    }
}
