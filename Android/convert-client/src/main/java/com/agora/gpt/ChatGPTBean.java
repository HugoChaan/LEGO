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
   private Usage usage;

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

   public Usage getUsage() {
      return usage;
   }

   public void setUsage(Usage usage) {
      this.usage = usage;
   }

   public static class Usage {
      private Integer prompt_tokens;
      private Integer completion_tokens;
      private Integer total_tokens;

      public Integer getPrompt_tokens() {
         return prompt_tokens;
      }

      public void setPrompt_tokens(Integer prompt_tokens) {
         this.prompt_tokens = prompt_tokens;
      }

      public Integer getCompletion_tokens() {
         return completion_tokens;
      }

      public void setCompletion_tokens(Integer completion_tokens) {
         this.completion_tokens = completion_tokens;
      }

      public Integer getTotal_tokens() {
         return total_tokens;
      }

      public void setTotal_tokens(Integer total_tokens) {
         this.total_tokens = total_tokens;
      }
   }
}
