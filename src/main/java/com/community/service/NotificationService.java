package com.community.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 异步通知服务
 *
 * 设计：将耗时的通知发送逻辑从主链路剥离，通过线程池异步执行
 * 使用场景：批量账单生成后异步推送站内信
 *
 * @Async 使用的线程池由 AsyncConfig.asyncExecutor() 提供
 */
@Slf4j
@Service
public class NotificationService {

    /**
     * 异步推送账单生成通知（站内信）
     *
     * @param ownerIds     需要通知的业主 ID 列表
     * @param feeItemName  费用项目名称
     * @param amount       金额
     * @param dueDate      截止日期
     */
    @Async("asyncExecutor")
    public void sendBillNotification(List<Long> ownerIds, String feeItemName,
                                    java.math.BigDecimal amount, String dueDate) {
        log.info("[异步通知] 开始推送账单通知 业主数={} 费用项目={} 金额={} 截止={}",
                ownerIds.size(), feeItemName, amount, dueDate);

        for (Long ownerId : ownerIds) {
            try {
                // 实际项目中此处调用站内信/短信服务
                // 此处用日志模拟通知发送
                log.debug("[站内信] → 业主ID={} 您的「{}」账单已生成，金额¥{}，请于{}前缴纳",
                        ownerId, feeItemName, amount, dueDate);
            } catch (Exception e) {
                log.error("[站内信] 发送失败 ownerId={}", ownerId, e);
                // 单条失败不影响其他业主的通知
            }
        }

        log.info("[异步通知] 账单通知推送完成 共{}条", ownerIds.size());
    }

    /**
     * 异步推送车位购买成功通知
     */
    @Async("asyncExecutor")
    public void sendParkingPurchaseNotification(Long ownerId, String spaceNo,
                                                 java.math.BigDecimal price) {
        log.info("[异步通知] 车位购买成功通知 ownerId={} 车位={} 价格={}", ownerId, spaceNo, price);
        try {
            log.debug("[站内信] → 业主ID={} 您购买的车位「{}」已成功，金额¥{}",
                    ownerId, spaceNo, price);
        } catch (Exception e) {
            log.error("[站内信] 车位购买通知发送失败 ownerId={}", ownerId, e);
        }
    }
}
