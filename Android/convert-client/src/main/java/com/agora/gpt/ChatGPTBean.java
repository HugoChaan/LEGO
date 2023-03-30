package com.agora.gpt;

/**
 * Created by 许成谱 on 3/22/23 4:35 下午.
 * qq:1550540124
 * 热爱生活每一天！
 */
class ChatGPTBean {

   private String id;
   private String answer;
   private Integer created;
   private String model;
   private String object;

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public String getAnswer() {
      return answer;
   }

   public void setAnswer(String answer) {
      this.answer = answer;
   }

   public Integer getCreated() {
      return created;
   }

   public void setCreated(Integer created) {
      this.created = created;
   }

   public String getModel() {
      return model;
   }

   public void setModel(String model) {
      this.model = model;
   }

   public String getObject() {
      return object;
   }

   public void setObject(String object) {
      this.object = object;
   }
}
