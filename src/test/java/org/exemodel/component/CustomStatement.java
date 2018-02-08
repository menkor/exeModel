package org.exemodel.component;

import org.exemodel.orm.Statement;

/**
 * Created by zp on 17/6/19.
 */
public class CustomStatement extends Statement<CustomStatement> {

    public static CustomStatement build(Class modelClass){
        CustomStatement customStatement = new CustomStatement();
        customStatement.setModelClass(modelClass);
        return customStatement;
    }

    public CustomStatement name(String value){
        return eq("name",value);
    }

    public CustomStatement age(int age){
        return eq("age",age);
    }

    public CustomStatement userId(int userId){
        return eq("userId",userId);
    }
}
