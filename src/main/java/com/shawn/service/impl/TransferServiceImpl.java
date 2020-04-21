package com.shawn.service.impl;

import com.shawn.annotation.Autowired;
import com.shawn.annotation.Qualifier;
import com.shawn.annotation.Service;
import com.shawn.annotation.Transactional;
import com.shawn.dao.AccountDao;
import com.shawn.pojo.Account;
import com.shawn.service.TransferService;

@Service
public class TransferServiceImpl implements TransferService {

    @Autowired
    @Qualifier("123")
    private AccountDao accountDao;


    @Override
    @Transactional
    public void transfer(String fromCardNo, String toCardNo, int money) throws Exception {

            Account from = accountDao.queryAccountByCardNo(fromCardNo);
            Account to = accountDao.queryAccountByCardNo(toCardNo);
            from.setMoney(from.getMoney()-money);
            to.setMoney(to.getMoney()+money);
            accountDao.updateAccountByCardNo(to);
            int c = 1/0;
            accountDao.updateAccountByCardNo(from);
    }
}
