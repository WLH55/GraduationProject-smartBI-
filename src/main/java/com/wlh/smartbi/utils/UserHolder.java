package com.wlh.smartbi.utils;


import com.wlh.smartbi.model.DTO.user.UserDTO;

/**
 * @author WLH
 * @className UserHolder
 * @date : 2023/05/04/ 16:13
 **/
public class UserHolder {
    public static ThreadLocal<UserDTO> user = new ThreadLocal<>();

    public static UserDTO getUser() {
        return user.get();
    }

    public static void setUser(UserDTO userDTO) {
        user.set(userDTO);
    }

}
