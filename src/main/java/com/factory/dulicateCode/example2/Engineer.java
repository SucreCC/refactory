package com.factory.dulicateCode.example2;

import java.util.Map;

/**
 * @Author: Sucre
 * @Date: 2021-08-24-08-01
 * @Description:
 */

public abstract class Engineer {
//    先给最小的
    private final Map<String, String> engineerList;

    private Engineer(Map<String, String> engineerList) {
        this.engineerList = engineerList;
    }

//    String find() {
//        for (Map.Entry<String, String> entry : engineerList.entrySet()) {
//            if (entry.getKey().equals(getType())) {
//                return entry.getValue();
//            }
//        }
//        return null;
//    }
//
//    看有没有其它包的实现
    protected abstract String getType();


}
