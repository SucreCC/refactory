package com.factory.longMethod.conditional.example1;

/**
 * @Author: dengKai
 * @Date: 2021-08-24-10-43
 * @Description: 遇见条件表达式
 */

public class Conditional {

    /**
     * 分解条件表达式
     * 特点：复杂的条件逻辑(一个if判断中有5个条件判断)  范围性判断  无法用枚举
     */

    void decomposeConditional() {
        int number = 10;
        int answer;

        // 重构前
        if (before(number) && after(number)) {
            answer = number * 100 + 1;
        } else {
            answer = (number + 100) * 100 + 10;
        }

        // 重构后
        if (after0AndBefore100(number)) {
            answer = number * 100 + 1;
        } else {
            answer = (number + 100) * 100 + 10;
        }
    }

    boolean before(int a) {
        return a <= 100;
    }

    boolean after(int a) {
        return a > 0;
    }

    boolean after0AndBefore100(int a) {
        return a > 0 && a <= 100;
    }

    /**
     * 合并条件表达式
     * 多条件判断导向相同结果
     */

    int consolidateConditional(int a) {
        // 重构前
        int b = 0;
        int c = 10;
        int d = 100;
        int answer = 1;
        if (a > b) {
            return answer;
        }
        if (a < c) {
            return answer;
        }
        if (a > d) {
            return answer;
        }

        // 重构后
        if (isaBoolean(a, b, c, d)) {
            return answer;
        }

        return answer;
    }

    private boolean isaBoolean(int a, int b, int c, int d) {
        return a > b || a < c || a > d;
    }

    /**
     * 合并重复条件判断
     * 多条件判断导向相同结果
     */

    void duplicateConditional(boolean flag) {
        double total;
        double price = 100;

        // 重构前
        if (flag) {
            total = 0.85 * price;
            send(total);
        } else {
            total = price;
            send(total);
        }

        // 重构后
        if (flag) {
            total = 0.85 * price;
        } else {
            total = price;
        }
        send(total);
    }

    private void send(double cost) {
        System.out.println("cost is" + cost);
    }

    /**
     * 使用break return continue 代替循环控制标记
     */

    void removeControlFlag() {
        byte[] words = {'a', 'b', 'c', 'd', 'e', 'f', 'g'};
        boolean flag = false;

        // 重构前
        for (int i = 0; i < words.length; ++i) {
            if (!flag) {
                if (words[i] == 'e') {
                    sendAlert();
                    flag = true;
                }
                if (words[i] == 'f') {
                    sendAlert();
                    flag = true;
                }
            }
        }

        for (int i = 0; i < words.length; ++i) {
            if (words[i] == 'e') {
                sendAlert();
                break;
            }
            if (words[i] == 'f') {
                sendAlert();
                break;
            }
        }


    }

    private void sendAlert() {
        System.out.println("=============");
    }


//        switch (originChatbot.getAuditState()) {
//        case CHANNEL_AUDITING:
//        case OPERATION_AUDITING:
//        case OPERATION_AUDIT_FAILED:
//        case CHANNEL_AUDIT_FAILED:
//        case UNKNOWN:
//            submitOperationApply(null, currentChatbot);
//            return;
//        case CHANNEL_AUDIT_PASS:
//            submitOperationApply(originChatbot, currentChatbot);
//            return;
//    }

    /**
     * 提取函数中的循环
     */

    public void testLoop() {
        byte[] words = {'a', 'b', 'c', 'd', 'e', 'f', 'g'};
        StringBuilder stringBuilder = new StringBuilder();
        int i = 0;
        while (true) {
            if (i > words.length) {
                break;
            }
            stringBuilder.append(words[i]);
            i++;
        }
    }
}

