package com.shop.receipt.config;

import com.shop.receipt.model.Order;
import com.shop.receipt.service.OrderService;
import com.shop.receipt.request.BoxberryStatusChecker;
import lombok.RequiredArgsConstructor;
import org.apache.log4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulingTasks {
    private final OrderService orderService;
    private static final Logger logger = Logger.getLogger(SchedulingTasks.class.getName());

    @Scheduled(cron = "0 0 9-21 * * *", zone = "Europe/Moscow")
    public void boxberryExecution() throws InterruptedException {
        Thread boxberry = new Thread("Boxberry Thread") {
            @Override
            public void run() {
                sendReceiptByBoxberryStatus();
            }
        };
        boxberry.start();
        logger.info("Boxberry Thread Start");
        boxberry.join();
        boxberry.interrupt();
        logger.info("Boxberry Thread Interrupted");
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "Europe/Moscow")
    public void robokassaReceiptStatusExecutor() throws InterruptedException {
        Thread robokassaStatus = new Thread("Robokassa Status Thread") {
            @Override
            public void run() {
                    checkRobokassaReceiptStatus();
            }
        };
        robokassaStatus.start();
        logger.info("Robokassa Thread Start");
        robokassaStatus.join();
        robokassaStatus.interrupt();
        logger.info("Robokassa Thread Interrupted");
    }

    private void checkRobokassaReceiptStatus() {
        logger.info("Robokassa Receipt Status Started".toUpperCase());
        int i = 0;
        for (Order order : orderService.getOrdersWithSentSecondReceipt()) {
            if (i++ > 49) { // because google has limit for write requests: 100 requests per 100 sec.
                synchronized (Thread.currentThread()) {
                    try {
                        i = 0;
                        logger.info("Before WAIT 100000");
                        Thread.currentThread().wait(100000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            logger.info(SchedulingTasks.class.getSimpleName() + " ROBOKASSA ORDER LIST FROM REPO: " + order.toString());
            orderService.checkFinalOFDStatus(order);
        }
    }

    private void sendReceiptByBoxberryStatus() {
        logger.info("Boxberry Status Executor started".toUpperCase());
        BoxberryStatusChecker boxberryStatusChecker = new BoxberryStatusChecker();
        boolean isDelivered;

        for (Order order : orderService.getBoxberryOrders()) {
            logger.info(SchedulingTasks.class.getSimpleName() + " BOXBERRY ORDER LIST FROM REPO: " + order.toString());
            isDelivered = boxberryStatusChecker.getBoxberryStatus(order.getId().toString());

            if (isDelivered) {
                orderService.updateOrderById(order.getId());
                logger.info(order.getId() + " receipt sent");
            }
        }
    }
}
