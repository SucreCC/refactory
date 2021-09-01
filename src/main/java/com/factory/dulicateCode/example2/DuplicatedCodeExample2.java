package com.factory.dulicateCode.example2;

import java.util.Map;

/**
 * @Author: dengKai
 * @Date: 2021-08-23-15-43
 * @Description: 同一一个类中出现相同的代码
 */

public class DuplicatedCodeExample2 {
    private final BackendEngineer backendEngineer;
    private final FrontendEngineer frontendEngineer;

    DuplicatedCodeExample2(Map<String, String> engineerList) {
        backendEngineer = new BackendEngineer(engineerList);
        frontendEngineer = new FrontendEngineer(engineerList);
    }

    String findBackendEngineer(){
        return backendEngineer.find();
    }

    String findFrontEngineer(){
        return frontendEngineer.find();
    }
}
