package com.agora.gpt;

import java.util.ArrayList;

/**
 * Created by 许成谱 on 4/13/23 3:44 下午.
 * qq:1550540124
 * 热爱生活每一天！
 * 定长集合，超出移除第一个
 */
class FixSizeList<T> extends ArrayList<T> {

    private  int capacity;

    public FixSizeList(int capacity) {
        super();
        if (capacity <= 1) {
            capacity = 2;
        }
        this.capacity = capacity;
    }

    public boolean add(T t) {
        // 超过长度，移除第一个
        if (size() >= capacity ) {
            super.remove(1);
        }
        return super.add(t);
    }

    public void setCapacity(int capacity){
        if (capacity <= 0) {
            capacity = 1;
        }
        this.capacity = capacity;
    }

    public void addSystemParams(T t){
        add(0,t);
    }

    public void removeAllHistory() {
        for (int i = 1; i < size(); i++) {
            remove(i);
        }
    }

    public void setMemoryLength(int length) {
        if (capacity <= 1) {
            length = 2;
        }
        this.capacity=length;
    }
}
