package com.community.delivery.domain.entity;

import com.community.delivery.domain.enums.NotificationChannel;
import jakarta.persistence.*;

import java.time.Instant;

/** 用户通知记录（短信 / 小程序） */
@Entity
@Table(name = "notification_record")
public class NotificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String orderNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationChannel channel;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 300)
    private String content;

    /** ARRIVED 到达通知 / REMINDER 超时提醒 / EXCEPTION 异常告知 */
    @Column(nullable = false, length = 16)
    private String notifyType;

    @Column(nullable = false)
    private boolean success = true;

    @Column(nullable = false, updatable = false)
    private Instant sentAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getNotifyType() { return notifyType; }
    public void setNotifyType(String notifyType) { this.notifyType = notifyType; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
}
