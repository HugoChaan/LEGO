package com.agora.gpt;

/**
 * Created by 许成谱 on 4/14/23 11:16 上午.
 * qq:1550540124
 * 热爱生活每一天！
 */
public enum ChatGptModel {

    GPT_35("gpt-3.5-turbo"), GPT_40("gpt-4");

    private String modelName;

    ChatGptModel(String modelName) {
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }
}
