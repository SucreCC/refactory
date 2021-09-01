package com.factory.dulicateCode.example1;

import java.util.Map;

/**
 * @Author: dengKai
 * @Date: 2021-08-23-15-43
 * @Description: 同一一个类中出现相同的代码
 */

public class DuplicatedCodeExample1 {
    private static final String BACKEND_ENGINEER = "Backend";
    private static final String FRONTEND_ENGINEER = "Frontend";
    private final Map<String, String> engineerList;

    DuplicatedCodeExample1(Map<String, String> engineerList) {
        this.engineerList = engineerList;
    }

    String findComputerEngineer(){
        return getEngineer("BACKEND_ENGINEER is not found", BACKEND_ENGINEER);
    }

    private String getEngineer(String s, String backendEngineer) {
        String engineer = s;
        for (Map.Entry<String, String> entry : engineerList.entrySet()) {
            if (entry.getKey().equals(backendEngineer)) {
                return entry.getValue();
            }
        }
        return engineer;
    }

    String findCivilEngineer(){
        return getEngineer("FRONTEND_ENGINEER is not found", FRONTEND_ENGINEER);
    }

//    String findByEngineerName(String engineerName){
//        String engineer = engineerName + "is not found";
//        for (Map.Entry<String,String> entry:engineerList.entrySet()){
//            if(entry.getKey().equals(engineerName)){
//                return entry.getValue();
//            }
//        }
//        return engineer;
//    }
}
