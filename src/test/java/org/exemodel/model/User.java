package org.exemodel.model;

import org.exemodel.orm.ExecutableModel;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Timestamp;
import java.util.Arrays;

/**
 * Created by zp on 2016/7/20.
 */
@Entity
@Table(name = "user")
public class User  extends ExecutableModel{
    private int id;
    private String name;
    private int age;
    private String details;
    private String idCard = "";
    private BigDecimal money;
    private InputStream image;
    private Timestamp createTime;
    private BigInteger serialNo;
    private byte[] pwd;

    @Id
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public BigDecimal getMoney() {
        return money;
    }

    public void setMoney(BigDecimal money) {
        this.money = money;
    }


    public Timestamp getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
    }

    public InputStream getImage() {
        return image;
    }

    public void setImage(InputStream image) {
        this.image = image;
    }

    public BigInteger getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(BigInteger serialNo) {
        this.serialNo = serialNo;
    }

    public byte[] getPwd() {
        return pwd;
    }

    public void setPwd(byte[] pwd) {
        this.pwd = pwd;
    }


    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", details='" + details + '\'' +
                ", idCard='" + idCard + '\'' +
                ", money=" + money +
                ", image=" + image +
                ", createTime=" + createTime +
                ", serialNo=" + serialNo +
                ", pwd=" + Arrays.toString(pwd) +
                '}';
    }
}
