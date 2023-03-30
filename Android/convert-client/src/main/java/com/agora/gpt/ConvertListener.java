package com.agora.gpt;

public interface ConvertListener {

    void onVoice2Text(int index,String text);

    void onText2Voice(int index,String pcmFilePath);

    void onQues2AnsSuccess(int index,String question, String answer);

    void onFailure(int errorCode, String message);
}
