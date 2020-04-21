package com.shawn.servlet;

import com.shawn.factory.BeanFactory;
import com.shawn.factory.ProxyFactory;
import com.shawn.pojo.Result;
import com.shawn.service.TransferService;
import com.shawn.utils.ComponentScanUtils;
import com.shawn.utils.JsonUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@WebServlet(name="transferServlet",urlPatterns = "/transferServlet")
public class TransferServlet extends HttpServlet {

    private TransferService transferService = null;

    @Override
    public void init(){
        ProxyFactory proxyFactory = (ProxyFactory) BeanFactory.getBean("ProxyFactory");
        transferService = (TransferService) BeanFactory.getBean("TransferService");

        // 如果存在TransferService则使用动态代理
        if (ComponentScanUtils.Scan("TransferService")) {

            //如果类实现了接口使用JDKProxy
            if (transferService.getClass().getInterfaces().length > 0) {
                transferService = (TransferService) proxyFactory.getJdkProxy(transferService);
            } else {
                transferService = (TransferService) proxyFactory.getCglibProxy(transferService);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        // 设置请求体的字符编码
        req.setCharacterEncoding("UTF-8");

        String fromCardNo = req.getParameter("fromCardNo");
        String toCardNo = req.getParameter("toCardNo");
        String moneyStr = req.getParameter("money");
        int money = Integer.parseInt(moneyStr);

        Result result = new Result();

        try {

            // 2. 调用service层方法
            transferService.transfer(fromCardNo,toCardNo,money);
            result.setStatus("200");
        } catch (Exception e) {
            e.printStackTrace();
            result.setStatus("201");
            result.setMessage(e.toString());
        }

        // 响应
        resp.setContentType("application/json;charset=utf-8");
        resp.getWriter().print(JsonUtils.object2Json(result));
    }
}
