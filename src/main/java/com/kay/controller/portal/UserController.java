package com.kay.controller.portal;

import com.kay.common.Const;
import com.kay.common.ResponseCode;
import com.kay.common.ServerResponse;
import com.kay.pojo.User;
import com.kay.service.IUserService;
import com.kay.util.CookieUtil;
import com.kay.util.JsonUtil;
import com.kay.util.RedisShardedPoolUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by kay on 2018/3/19.
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private IUserService iUserService;

    /**
     *用户登录
     * @param username
     * @param password
     * @return
     */
    @PostMapping("/login")
    public ServerResponse<User> login(String username, String password) {
        return iUserService.login(username, password);
    }

//    /**
//     * 登出
//     */
//    @PostMapping("/logout")
//    public ServerResponse<String> logout(HttpServletRequest request,HttpServletResponse response){
////        String loginToken = CookieUtil.readLoginToken(request);
////        CookieUtil.delLoginToken(request, response);
////        RedisShardedPoolUtil.del(loginToken);
//        return ServerResponse.createBySuccess();
//    }

    /**
     * 注册
     * @param user
     * @return
     */
    @PostMapping("/register")
    public ServerResponse<String> register(User user) {
        return iUserService.register(user);
    }

    /**
     * 验证用户名和email是否已经存在
     * @param str
     * @param type
     * @return
     */
    @PostMapping("/check_valid")
    public ServerResponse<String> checkUserName(String str,String type){
        return iUserService.checkValid(str,type);
    }

    /**
     * 忘记密码获取问题
     * @param username
     * @return
     */
    @PostMapping("/forget_get_question")
    public ServerResponse<String> forgetGetQuestion(String username) {
         return iUserService.forgetGetQuestion(username);
    }

    /**
     * 忘记密码验证回答
     * @param username
     * @param question
     * @param answer
     * @return
     */
    @PostMapping("/forget_check_answer")
    public ServerResponse<String> forgetCheckAnswer(String username, String question, String answer) {
        return iUserService.checkQuestionAnswer(username, question, answer);
    }

    /**
     *验证通过后重置密码，带着上次返回的token
     * @param username
     * @param passwordNew
     * @param forgetToken
     * @return
     */
    @PostMapping("/forget_reset_password")
    public ServerResponse<String> forgetResetPassword(String username, String passwordNew, String forgetToken) {
        return iUserService.forgetResetPassword(username, passwordNew, forgetToken);
    }

    /**
     * 已登录用户的重置密码
     * @param passwordOld
     * @param passwordNew
     * @param request
     * @return
     */
    @PostMapping("/reset_password")
    public ServerResponse<String> resetPassword(String passwordOld, String passwordNew, HttpServletRequest request) {
        String loginToken = CookieUtil.readLoginToken(request);
        if (StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录");
        }
        User user = JsonUtil.string2obj(RedisShardedPoolUtil.get(loginToken), User.class);

        if (user == null) {
            return ServerResponse.createByErrorMessage("用户未登录");
        }
        return iUserService.resetPassword(passwordOld, passwordNew, user);
    }

    /**
     * 更新用户信息-----注意横向越权问题
     * @param request
     * @param user
     * @return
     */
    @PostMapping("/update_information")
    public ServerResponse updateInformation(HttpServletRequest request, User user) {
        String loginToken = CookieUtil.readLoginToken(request);
        if (StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录");
        }
        User currentUser = JsonUtil.string2obj(RedisShardedPoolUtil.get(loginToken), User.class);

        //此处用户id前端可能回传过来，也有可能不传，所以设置一下保险
        //同时此处也为了防止横向越权的问题，即传过来的id并不是当前登录的用户id
        user.setId(currentUser.getId());
        user.setUsername(currentUser.getUsername()); //username 不能被更新
        ServerResponse response=iUserService.updateUserInfo(user);
        if (response.isSuccess()) {
//            session.setAttribute(Const.CURRENT_USER,response.getData());
            //更新Redis中用户信息
            RedisShardedPoolUtil.setEx(loginToken, JsonUtil.obj2string(response.getData()), Const.RedisCacheExTime.REDIS_SESSION_EXTIME);
            return ServerResponse.createBySuccessMessage("更新成功");
        }
        return response;
    }

    /**
     * 获取当前用户信息
     * @return
     */
    @PostMapping(value = "/get_user_info")
    public ServerResponse<User> getUserInfo(HttpServletRequest request) {
        String loginToken = CookieUtil.readLoginToken(request);
        if (StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录,无法获取当前用户的信息");
        }
        User user = JsonUtil.string2obj(RedisShardedPoolUtil.get(loginToken), User.class);
        if(user != null){
            return ServerResponse.createBySuccess(user);
        }
        return ServerResponse.createByErrorMessage("用户未登录,无法获取当前用户的信息");
    }

    /**
     * 获取当前用户详细信息
     * @param request
     * @return
     */
    @PostMapping(value = "/get_information")
    public ServerResponse<User> get_information(HttpServletRequest request){
        String loginToken = CookieUtil.readLoginToken(request);
        if (StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录,无法获取当前用户的信息");
        }
        User user = JsonUtil.string2obj(RedisShardedPoolUtil.get(loginToken), User.class);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"未登录,需要强制登录status=10");
        }
        return iUserService.getUserInfo(user.getId());
    }
}
