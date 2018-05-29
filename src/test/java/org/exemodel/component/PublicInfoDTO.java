package org.exemodel.component;

import org.exemodel.plugin.Transferable;
import org.exemodel.util.BitMapUtil;

/**
 * Created by xiaofengxu on 18/5/29.
 * if mobile public to all ,set true else false
 */
public class PublicInfoDTO implements Transferable<PublicInfoDTO,Integer> {
    private boolean mobile;
    private boolean name;
    private boolean gender;
    private boolean idCard;
    private boolean address;

    public boolean isMobile() {
        return mobile;
    }

    public void setMobile(boolean mobile) {
        this.mobile = mobile;
    }

    public boolean isName() {
        return name;
    }

    public void setName(boolean name) {
        this.name = name;
    }

    public boolean isGender() {
        return gender;
    }

    public void setGender(boolean gender) {
        this.gender = gender;
    }

    public boolean isIdCard() {
        return idCard;
    }

    public void setIdCard(boolean idCard) {
        this.idCard = idCard;
    }

    public boolean isAddress() {
        return address;
    }

    public void setAddress(boolean address) {
        this.address = address;
    }

    @Override
    public Integer to() {
        return BitMapUtil.intBitMap(this);
    }

    @Override
    public PublicInfoDTO from(Integer des) {
        return BitMapUtil.fillDTO(des,PublicInfoDTO.class);
    }
}
