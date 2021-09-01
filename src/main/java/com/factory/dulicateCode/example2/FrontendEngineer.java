package com.factory.dulicateCode.example2;

import java.util.Map;

/**
 * @Author: Sucre
 * @Date: 2021-08-24-08-06
 * @Description:
 */

public class FrontendEngineer extends Engineer {
    public FrontendEngineer(Map<String, String> engineerList) {
        super(engineerList);
    }

    String find() {
        for (Map.Entry<String, String> entry : engineerList.entrySet()) {
            if (entry.getKey().equals("Frontend")) {
                return entry.getValue();
            }
        }
        return null;
    }

//    @Override
//    String find(){
//        return super.find();
//    }
//
//    @Override
//    String getType() {
//        return "Frontend";
//    }
}
