package com.jeesite.modules.js.web;

import com.jeesite.common.service.ServiceException;
import com.jeesite.modules.common.utils.PasswordUtil;
import com.jeesite.modules.common.utils.RedisUtils;
import com.jeesite.modules.js.entity.JsUser;
import com.jeesite.modules.js.entity.Question;
import com.jeesite.modules.js.entity.QuestionTasks;
import com.jeesite.modules.js.entity.other.LoginRsp;
import com.jeesite.modules.js.service.JsUserService;
import com.jeesite.modules.js.service.QuestionService;
import com.jeesite.modules.js.service.QuestionTasksService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.UUID;


/***
 * 前端专用接口
 * 2018.11.5
 * author: jo
 */
@RestController
@RequestMapping(value = "${adminPath}/third")
public class ThirdController {
    @Autowired
    private QuestionService questionService;
    @Autowired
    private QuestionTasksService questionTasksService;
    @Autowired
    private JsUserService jsUserService;
    @Autowired
    private RedisUtils redisUtils;

    public static final String AUTHORIZATION = "Authorization";

    public HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    /***
     * 获取当前用户
     */
    private LoginRsp getUser () {
        LoginRsp loginRsp = null;
        HttpServletRequest request = getRequest();
        String sessionId = request.getHeader(AUTHORIZATION);
        if (StringUtils.isBlank(sessionId)) {
            throw new ServiceException("Header参数有误");
        }
        loginRsp = redisUtils.getSession(sessionId);
        if (loginRsp == null) {
            throw new ServiceException("请重新登录!");
        }
        return loginRsp;
    }

    /***
     * 获取随机一个题目
     */
    @RequestMapping("/getRandomQuestion")
    public Question getRandomQues(String userId) {
        Question question = new Question();
        QuestionTasks questionTasks = new QuestionTasks();

        //获取随机题目
        question = questionService.getRandomQuestion("aaa");
        //获取题目的tasks
        String questionId = question.getId();
        questionTasks.setQuestionId(questionId);
        List<QuestionTasks> tasks = questionTasksService.findList(questionTasks);
        question.setQuestionTasksList(tasks);
        return question;
    }
    /***
     * 获取指定题目
     */
    @RequestMapping("/getQuestion/{id}")
    public Question getQuestion(@PathVariable String id) {
        QuestionTasks questionTasks = new QuestionTasks();
        Question question = questionService.get(id);
        //获取题目的tasks/.
        questionTasks.setQuestionId(id);
        List<QuestionTasks> tasks = questionTasksService.findList(questionTasks);
        question.setQuestionTasksList(tasks);
        return question;
    };

    /***
     * 保存前端传来的答案 18.11.27
     */
    @RequestMapping("/saveAnswer")
    public void saveAnswer() {

    }

    /***
     * 登录
     */
    @RequestMapping("/login")
    public LoginRsp login(@RequestBody JsUser user) {

        JsUser temp = new JsUser();
        temp.setMobile(user.getMobile());
        List<JsUser> dbUser = jsUserService.findList(temp);
        if (dbUser.size() != 0) {
            String DbPassWord = dbUser.get(0).getPassword();
            Boolean isPass = PasswordUtil.valid(user.getPassword(), Long.parseLong(user.getMobile()), DbPassWord);
            if (isPass) {
                String token = UUID.randomUUID().toString();
                LoginRsp loginRsp = new LoginRsp(token, dbUser.get(0));
                redisUtils.setSession(token, loginRsp);
                return loginRsp;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /***
     * 注册
     */
    @RequestMapping("/sign")
    public String sign(@RequestBody JsUser user) {
        JsUser temp = new JsUser();
        temp.setMobile(user.getMobile());
        if (jsUserService.findList(temp).size() > 0) {
            return "该手机号码已被使用!";
        }
        user.setRank(0);
        user.setCreateDate(new Date());
        String md5Password =  PasswordUtil.getMd5PasswordOnce(user.getPassword(), Long.parseLong(user.getMobile()));
        user.setPassword(md5Password);
        jsUserService.save(user);
        return "注册成功咯,请登录!";
    }

    @RequestMapping("/getUserInfo")
    public LoginRsp getUserInfo() {
        return getUser();
    }



}
